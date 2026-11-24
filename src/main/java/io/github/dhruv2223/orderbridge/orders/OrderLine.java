package io.github.dhruv2223.orderbridge.orders;

import java.math.BigDecimal;

import org.springframework.data.relational.core.mapping.Table;

/** Part of the {@link PurchaseOrder} aggregate; has no identity or repository of its own. */
@Table("order_line")
public record OrderLine(String sku, int quantity, BigDecimal unitPrice) {

	BigDecimal lineTotal() {
		return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
	}

}
