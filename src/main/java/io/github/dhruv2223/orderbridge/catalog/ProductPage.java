package io.github.dhruv2223.orderbridge.catalog;

import java.util.List;

import org.springframework.data.domain.Page;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Paged envelope. JSON: {@code {"items":[...],"page":0,...}}. XML:
 * {@code <products page="0" size="20" totalElements="4" totalPages="1"><product>...</product></products>}.
 */
@JacksonXmlRootElement(localName = "products")
public record ProductPage(
		@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "product") List<ProductResponse> items,
		@JacksonXmlProperty(isAttribute = true) int page,
		@JacksonXmlProperty(isAttribute = true) int size,
		@JacksonXmlProperty(isAttribute = true) long totalElements,
		@JacksonXmlProperty(isAttribute = true) int totalPages) {

	static ProductPage from(Page<Product> page) {
		return new ProductPage(page.map(ProductResponse::from).getContent(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

}
