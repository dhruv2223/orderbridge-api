package io.github.dhruv2223.orderbridge.catalog;

import java.math.RoundingMode;

import io.github.dhruv2223.orderbridge.fx.ExchangeRateService;
import io.github.dhruv2223.orderbridge.web.BadRequestException;
import io.github.dhruv2223.orderbridge.web.ConflictException;
import io.github.dhruv2223.orderbridge.web.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository products;

	private final ExchangeRateService exchangeRates;

	ProductService(ProductRepository products, ExchangeRateService exchangeRates) {
		this.products = products;
		this.exchangeRates = exchangeRates;
	}

	@Transactional(readOnly = true)
	public Page<Product> list(String category, int page, int size) {
		var pageable = PageRequest.of(page, size, Sort.by("sku"));
		return (category == null || category.isBlank()) ? this.products.findAll(pageable)
				: this.products.findByCategory(category, pageable);
	}

	@Transactional(readOnly = true)
	public Product get(String sku) {
		return this.products.findBySku(sku).orElseThrow(() -> new NotFoundException("Product", sku));
	}

	@Transactional
	public Product create(ProductRequest request) {
		if (this.products.existsBySku(request.sku())) {
			throw new ConflictException("Product '" + request.sku() + "' already exists");
		}
		return this.products.save(Product.create(request.sku(), request.name(), request.description(),
				request.category(), request.price(), request.stock()));
	}

	@Transactional
	public Product replace(String sku, ProductRequest request) {
		if (!sku.equals(request.sku())) {
			throw new BadRequestException("SKU in body (" + request.sku() + ") does not match URL (" + sku + ")");
		}
		var updated = get(sku).withDetails(request.name(), request.description(), request.category(),
				request.price(), request.stock());
		return this.products.save(updated);
	}

	@Transactional
	public void delete(String sku) {
		this.products.delete(get(sku));
	}

	/** Upserts every item of a supplier feed in one transaction: all or nothing. */
	@Transactional
	public ImportReport importFeed(CatalogFeed feed) {
		int created = 0;
		int updated = 0;
		for (var item : feed.items()) {
			var existing = this.products.findBySku(item.sku());
			if (existing.isPresent()) {
				this.products.save(existing.get()
					.withDetails(item.name(), item.description(), item.category(), item.price(), item.stock()));
				updated++;
			}
			else {
				this.products.save(Product.create(item.sku(), item.name(), item.description(), item.category(),
						item.price(), item.stock()));
				created++;
			}
		}
		return new ImportReport(feed.supplier(), created, updated);
	}

	public PriceQuote quote(String sku, String currency) {
		var product = get(sku);
		var rate = this.exchangeRates.rate(Product.CURRENCY, currency);
		var converted = product.price().multiply(rate.value()).setScale(2, RoundingMode.HALF_EVEN);
		return new PriceQuote(sku, product.price(), Product.CURRENCY, converted, currency, rate.value(), rate.date());
	}

}
