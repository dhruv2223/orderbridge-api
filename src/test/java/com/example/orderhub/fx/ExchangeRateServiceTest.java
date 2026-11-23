package com.example.orderhub.fx;

import java.time.LocalDate;

import com.example.orderhub.web.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Exercises the real declarative client and JSON parsing against a mock HTTP server.
 */
class ExchangeRateServiceTest {

	private MockRestServiceServer server;

	private ExchangeRateService service;

	@BeforeEach
	void setUp() {
		var builder = RestClient.builder().baseUrl("https://fx.test/v1");
		this.server = MockRestServiceServer.bindTo(builder).build();
		this.service = new ExchangeRateService(FxConfiguration.create(builder));
	}

	@Test
	void parsesRateFromJsonResponse() {
		this.server.expect(requestTo("https://fx.test/v1/latest?base=EUR&symbols=USD"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
					{"amount":1.0,"base":"EUR","date":"2026-11-24","rates":{"USD":1.0512}}
					""", MediaType.APPLICATION_JSON));

		var rate = this.service.rate("EUR", "USD");

		assertThat(rate.value()).isEqualByComparingTo("1.0512");
		assertThat(rate.date()).isEqualTo(LocalDate.of(2026, 11, 24));
		this.server.verify();
	}

	@Test
	void sameCurrencyNeedsNoRemoteCall() {
		assertThat(this.service.rate("EUR", "EUR").value()).isEqualByComparingTo("1");
		this.server.verify();
	}

	@Test
	void upstreamFailureBecomesFxUnavailable() {
		this.server.expect(requestTo("https://fx.test/v1/latest?base=EUR&symbols=USD")).andRespond(withServerError());

		assertThatExceptionOfType(FxUnavailableException.class).isThrownBy(() -> this.service.rate("EUR", "USD"));
	}

	@Test
	void missingCurrencyInResponseIsBadRequest() {
		this.server.expect(requestTo("https://fx.test/v1/latest?base=EUR&symbols=XYZ"))
			.andRespond(withSuccess("""
					{"amount":1.0,"base":"EUR","date":"2026-11-24","rates":{}}
					""", MediaType.APPLICATION_JSON));

		assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> this.service.rate("EUR", "XYZ"))
			.withMessageContaining("XYZ");
	}

}
