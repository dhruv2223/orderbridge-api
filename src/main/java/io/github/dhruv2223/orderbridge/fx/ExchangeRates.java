package io.github.dhruv2223.orderbridge.fx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * JSON payload returned by the Frankfurter API, e.g.
 * {@code {"amount":1.0,"base":"EUR","date":"2026-11-24","rates":{"USD":1.0512}}}.
 */
public record ExchangeRates(BigDecimal amount, String base, LocalDate date, Map<String, BigDecimal> rates) {
}
