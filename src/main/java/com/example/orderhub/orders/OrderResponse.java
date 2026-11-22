package com.example.orderhub.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.example.orderhub.catalog.Product;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "order")
public record OrderResponse(
		@JacksonXmlProperty(isAttribute = true) Long id,
		String customerReference,
		OrderStatus status,
		BigDecimal total,
		String currency,
		Instant createdAt,
		@JacksonXmlElementWrapper(localName = "lines") @JacksonXmlProperty(localName = "line") List<Line> lines) {

	public record Line(String sku, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
	}

	static OrderResponse from(PurchaseOrder order) {
		var lines = order.lines()
			.stream()
			.map(l -> new Line(l.sku(), l.quantity(), l.unitPrice(), l.lineTotal()))
			.toList();
		return new OrderResponse(order.id(), order.customerReference(), order.status(), order.total(),
				Product.CURRENCY, order.createdAt(), lines);
	}

}
