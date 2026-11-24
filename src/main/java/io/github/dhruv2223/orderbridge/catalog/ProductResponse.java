package io.github.dhruv2223.orderbridge.catalog;

import java.math.BigDecimal;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Public representation of a product. In XML the SKU is rendered as an attribute:
 * {@code <product sku="KB-100"><name>...</name>...</product>}.
 */
@JacksonXmlRootElement(localName = "product")
public record ProductResponse(
		@JacksonXmlProperty(isAttribute = true) String sku,
		String name,
		String description,
		String category,
		BigDecimal price,
		String currency,
		int stock) {

	static ProductResponse from(Product product) {
		return new ProductResponse(product.sku(), product.name(), product.description(), product.category(),
				product.price(), Product.CURRENCY, product.stock());
	}

}
