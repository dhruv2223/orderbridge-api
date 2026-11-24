package io.github.dhruv2223.orderbridge.fx;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.dhruv2223.orderbridge.web.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class ExchangeRateService {

	private final ExchangeRateClient client;

	ExchangeRateService(ExchangeRateClient client) {
		this.client = client;
	}

	public Rate rate(String from, String to) {
		if (from.equals(to)) {
			return new Rate(BigDecimal.ONE, LocalDate.now());
		}
		ExchangeRates rates;
		try {
			rates = this.client.latest(from, to);
		}
		catch (RestClientException ex) {
			throw new FxUnavailableException("Exchange rate service is unavailable", ex);
		}
		if (rates == null || rates.rates() == null || !rates.rates().containsKey(to)) {
			throw new BadRequestException("Unsupported currency: " + to);
		}
		return new Rate(rates.rates().get(to), rates.date());
	}

	public record Rate(BigDecimal value, LocalDate date) {
	}

}
