package com.example.orderhub.orders;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.orderhub.catalog.Product;
import com.example.orderhub.catalog.ProductRepository;
import com.example.orderhub.web.ConflictException;
import com.example.orderhub.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private final OrderRepository orders;

	private final ProductRepository products;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	@Autowired
	OrderService(OrderRepository orders, ProductRepository products, ApplicationEventPublisher events) {
		this(orders, products, events, Clock.systemUTC());
	}

	OrderService(OrderRepository orders, ProductRepository products, ApplicationEventPublisher events, Clock clock) {
		this.orders = orders;
		this.products = products;
		this.events = events;
		this.clock = clock;
	}

	/**
	 * Validates stock for every line, reserves it and stores the order in one transaction.
	 * Duplicate SKUs in the request are merged. If any line fails, nothing is changed.
	 */
	@Transactional
	public PurchaseOrder place(OrderRequest request) {
		Map<String, Integer> quantities = new LinkedHashMap<>();
		request.lines().forEach(line -> quantities.merge(line.sku(), line.quantity(), Integer::sum));

		var checks = quantities.entrySet().stream().map(e -> check(e.getKey(), e.getValue())).toList();

		List<String> violations = new ArrayList<>();
		List<OrderLine> lines = new ArrayList<>();
		for (var check : checks) {
			switch (check) {
				case StockCheck.Available(Product product, int quantity) -> {
					this.products.save(product.withStock(product.stock() - quantity));
					lines.add(new OrderLine(product.sku(), quantity, product.price()));
				}
				case StockCheck.Insufficient(var sku, var requested, var available) ->
					violations.add("Insufficient stock for " + sku + ": requested " + requested + ", available " + available);
				case StockCheck.UnknownSku(var sku) -> violations.add("Unknown product " + sku);
			}
		}
		if (!violations.isEmpty()) {
			// Throwing rolls back the stock already reserved for earlier lines.
			throw new OrderRejectedException(violations);
		}

		var saved = this.orders.save(PurchaseOrder.place(request.customerReference(), lines, Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS)));
		this.events.publishEvent(new OrderPlaced(saved.id(), saved.customerReference(), saved.total(),
				lines.stream().map(OrderLine::sku).toList()));
		return saved;
	}

	private StockCheck check(String sku, int quantity) {
		return this.products.findBySku(sku)
			.<StockCheck>map(p -> p.stock() >= quantity ? new StockCheck.Available(p, quantity)
					: new StockCheck.Insufficient(sku, quantity, p.stock()))
			.orElseGet(() -> new StockCheck.UnknownSku(sku));
	}

	@Transactional(readOnly = true)
	public PurchaseOrder get(long id) {
		return this.orders.findById(id).orElseThrow(() -> new NotFoundException("Order", id));
	}

	@Transactional(readOnly = true)
	public List<PurchaseOrder> list(String customerReference) {
		return (customerReference == null || customerReference.isBlank()) ? this.orders.findTop50ByOrderByCreatedAtDesc()
				: this.orders.findByCustomerReferenceOrderByCreatedAtDesc(customerReference);
	}

	/** Moves an order through its lifecycle; cancelling returns the reserved stock. */
	@Transactional
	public PurchaseOrder changeStatus(long id, OrderStatus next) {
		var order = get(id);
		PurchaseOrder updated;
		try {
			updated = order.withStatus(next);
		}
		catch (IllegalStateException ex) {
			throw new ConflictException(ex.getMessage());
		}
		if (next == OrderStatus.CANCELLED) {
			order.lines()
				.forEach(line -> this.products.findBySku(line.sku())
					.ifPresent(p -> this.products.save(p.withStock(p.stock() + line.quantity()))));
		}
		return this.orders.save(updated);
	}

}
