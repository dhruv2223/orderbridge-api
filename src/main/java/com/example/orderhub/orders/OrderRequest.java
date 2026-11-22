package com.example.orderhub.orders;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * JSON: {@code {"customerReference":"ACME-42","lines":[{"sku":"KB-100","quantity":2}]}}
 * <br>
 * XML: {@code <order><customerReference>ACME-42</customerReference><lines><line><sku>KB-100</sku><quantity>2</quantity></line></lines></order>}
 */
@JacksonXmlRootElement(localName = "order")
public record OrderRequest(
		@NotBlank @Size(max = 64) String customerReference,
		@JacksonXmlElementWrapper(localName = "lines") @JacksonXmlProperty(localName = "line") @NotEmpty @Size(max = 50) List<@Valid Line> lines) {

	public record Line(@NotBlank String sku, @NotNull @Positive @Max(1000) Integer quantity) {
	}

}
