package io.github.dhruv2223.orderbridge.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;

import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "priceQuote")
public record PriceQuote(String sku, BigDecimal basePrice, String baseCurrency, BigDecimal price, String currency,
		BigDecimal exchangeRate, LocalDate rateDate) {
}
