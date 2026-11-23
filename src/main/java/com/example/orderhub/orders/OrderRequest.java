package com.example.orderhub.orders;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public record OrderRequest(@NotBlank @Size(max = 64) String customerReference,
		@NotEmpty @Size(max = 50) List<@Valid Line> lines) {

	/**
	 * Explicit creator: with records, renaming wrapped XML items ({@code <lines><line>})
	 * on the component also renames the constructor parameter, which breaks binding in
	 * jackson-dataformat-xml. Naming the parameter explicitly keeps it as "lines".
	 */
	@JsonCreator
	public OrderRequest(@JsonProperty("customerReference") String customerReference,
			@JsonProperty("lines") @JacksonXmlProperty(localName = "lines") List<@Valid Line> lines) {
		this.customerReference = customerReference;
		this.lines = lines;
	}

	@Override
	@JacksonXmlElementWrapper(localName = "lines")
	@JacksonXmlProperty(localName = "line")
	public List<Line> lines() {
		return this.lines;
	}

	public record Line(@NotBlank String sku, @NotNull @Positive @Max(1000) Integer quantity) {
	}

}
