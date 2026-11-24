package io.github.dhruv2223.orderbridge.catalog;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Catalog product, persisted with Spring Data JDBC. Immutable: changes produce a copy,
 * and {@link Version} gives optimistic locking so concurrent stock updates cannot be lost.
 */
@Table("product")
public record Product(@Id Long id, String sku, String name, String description, String category, BigDecimal price,
		int stock, @Version Integer version) {

	public static final String CURRENCY = "EUR";

	static Product create(String sku, String name, String description, String category, BigDecimal price, int stock) {
		return new Product(null, sku, name, description, category, price, stock, null);
	}

	Product withDetails(String name, String description, String category, BigDecimal price, int stock) {
		return new Product(this.id, this.sku, name, description, category, price, stock, this.version);
	}

	public Product withStock(int newStock) {
		if (newStock < 0) {
			throw new IllegalStateException("Stock for " + this.sku + " cannot go negative");
		}
		return new Product(this.id, this.sku, this.name, this.description, this.category, this.price, newStock,
				this.version);
	}

}
