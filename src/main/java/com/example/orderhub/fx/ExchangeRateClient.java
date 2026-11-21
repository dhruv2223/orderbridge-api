package com.example.orderhub.fx;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative HTTP client: Spring generates the implementation at runtime on top of
 * {@link org.springframework.web.client.RestClient}.
 */
@HttpExchange(accept = "application/json")
public interface ExchangeRateClient {

	@GetExchange("/latest")
	ExchangeRates latest(@RequestParam("base") String base, @RequestParam("symbols") String symbols);

}
