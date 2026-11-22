package com.example.orderhub.orders;

import com.example.orderhub.catalog.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to {@link OrderPlaced} after the transaction commits, decoupled from the
 * ordering code through Spring's application event API.
 */
@Component
class LowStockMonitor {

	private static final Logger log = LoggerFactory.getLogger(LowStockMonitor.class);

	private final ProductRepository products;

	private final int threshold;

	LowStockMonitor(ProductRepository products, @Value("${orderhub.low-stock-threshold:5}") int threshold) {
		this.products = products;
		this.threshold = threshold;
	}

	@TransactionalEventListener
	void onOrderPlaced(OrderPlaced event) {
		log.info("Order {} placed by {} for {} EUR", event.orderId(), event.customerReference(), event.total());
		event.skus()
			.forEach(sku -> this.products.findBySku(sku)
				.filter(p -> p.stock() <= this.threshold)
				.ifPresent(p -> log.warn("Low stock for {}: {} left", p.sku(), p.stock())));
	}

}
