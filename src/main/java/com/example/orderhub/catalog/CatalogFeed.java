package com.example.orderhub.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Bulk catalog feed as delivered by suppliers in XML, mixing attributes and elements:
 *
 * <pre>{@code
 * <catalogFeed supplier="ACME" generatedAt="2026-11-25T08:00:00Z">
 *   <item sku="KB-100">
 *     <name>Mechanical Keyboard</name>
 *     <category>peripherals</category>
 *     <price>84.90</price>
 *     <stock>60</stock>
 *   </item>
 * </catalogFeed>
 * }</pre>
 */
@JacksonXmlRootElement(localName = "catalogFeed")
public record CatalogFeed(
		@JacksonXmlProperty(isAttribute = true) @NotBlank String supplier,
		@JacksonXmlProperty(isAttribute = true) Instant generatedAt,
		@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "item") @NotEmpty @Size(max = 500) List<@Valid Item> items) {

	public record Item(
			@JacksonXmlProperty(isAttribute = true) @NotBlank @Pattern(regexp = "[A-Z0-9-]{3,32}") String sku,
			@NotBlank @Size(max = 120) String name,
			@Size(max = 1000) String description,
			@NotBlank @Size(max = 60) String category,
			@NotNull @DecimalMin("0.00") BigDecimal price,
			@NotNull @PositiveOrZero Integer stock) {
	}

}
