package io.github.dhruv2223.orderbridge.fx;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
class FxConfiguration {

	@Bean
	ExchangeRateClient exchangeRateClient(RestClient.Builder builder, @Value("${orderbridge.fx.base-url}") String baseUrl) {
		var requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(2));
		requestFactory.setReadTimeout(Duration.ofSeconds(5));
		return create(builder.requestFactory(requestFactory).baseUrl(baseUrl));
	}

	/** Builds the proxy from any builder, so tests can bind a mock server to it. */
	static ExchangeRateClient create(RestClient.Builder builder) {
		var adapter = RestClientAdapter.create(builder.build());
		return HttpServiceProxyFactory.builderFor(adapter).build().createClient(ExchangeRateClient.class);
	}

}
