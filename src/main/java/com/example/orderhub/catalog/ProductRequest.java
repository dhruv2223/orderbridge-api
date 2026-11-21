package com.example.orderhub.catalog;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Create/replace payload. Accepted as JSON or as XML ({@code <product>...</product>}).
 */
@JacksonXmlRootElement(localName = "product")
public record ProductRequest(
		@NotBlank @Pattern(regexp = "[A-Z0-9-]{3,32}", message = "must be 3-32 upper-case letters, digits or dashes") String sku,
		@NotBlank @Size(max = 120) String name,
		@Size(max = 1000) String description,
		@NotBlank @Size(max = 60) String category,
		@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
		@NotNull @PositiveOrZero Integer stock) {
}
