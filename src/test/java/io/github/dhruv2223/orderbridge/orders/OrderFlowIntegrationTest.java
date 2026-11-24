package io.github.dhruv2223.orderbridge.orders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import io.github.dhruv2223.orderbridge.TestcontainersConfiguration;
import io.github.dhruv2223.orderbridge.catalog.ProductRepository;
import io.github.dhruv2223.orderbridge.fx.ExchangeRateClient;
import io.github.dhruv2223.orderbridge.fx.ExchangeRates;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Full application against a real Postgres (Testcontainers) with Flyway migrations:
 * HTTP in, JSON/XML conversion, security, transactions and Spring Data JDBC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderFlowIntegrationTest {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ProductRepository products;

	@MockitoBean
	ExchangeRateClient exchangeRateClient;

	@Test
	void placesXmlOrderReservesStockAndReadsBackAsXml() {
		var sku = newProduct("19.99", 10);

		var placed = this.mvc.post()
			.uri("/api/orders")
			.with(httpBasic("partner", "partner-secret"))
			.contentType(MediaType.APPLICATION_XML)
			.accept(MediaType.APPLICATION_XML)
			.content("""
					<order>
					  <customerReference>ACME-1</customerReference>
					  <lines>
					    <line><sku>%1$s</sku><quantity>2</quantity></line>
					    <line><sku>%1$s</sku><quantity>1</quantity></line>
					  </lines>
					</order>
					""".formatted(sku))
			.exchange();

		assertThat(placed).hasStatus(HttpStatus.CREATED).hasContentTypeCompatibleWith(MediaType.APPLICATION_XML);
		assertThat(placed).bodyText()
			.contains("<status>PLACED</status>")
			.contains("<total>59.97</total>")
			.contains("<line><sku>" + sku + "</sku><quantity>3</quantity>");
		assertThat(stockOf(sku)).isEqualTo(7);

		var location = placed.getResponse().getHeader("Location");
		assertThat(this.mvc.get().uri(location).with(httpBasic("partner", "partner-secret")))
			.hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"customerReference":"ACME-1","status":"PLACED","total":59.97,"currency":"EUR"}
					""");
	}

	@Test
	void rejectedOrderRollsBackEveryLine() {
		var plenty = newProduct("5.00", 100);
		var scarce = newProduct("5.00", 1);

		var result = this.mvc.post()
			.uri("/api/orders")
			.with(httpBasic("partner", "partner-secret"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"customerReference":"ACME-2","lines":[
					  {"sku":"%s","quantity":10},{"sku":"%s","quantity":5},{"sku":"NOPE-1","quantity":1}]}
					""".formatted(plenty, scarce));

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
			.bodyJson()
			.extractingPath("$.violations")
			.asArray()
			.containsExactly("Insufficient stock for " + scarce + ": requested 5, available 1", "Unknown product NOPE-1");
		assertThat(stockOf(plenty)).as("stock reserved for the valid line is rolled back").isEqualTo(100);
	}

	@Test
	void cancellingAnOrderReturnsStock() {
		var sku = newProduct("1.00", 5);
		var placed = this.mvc.post()
			.uri("/api/orders")
			.with(httpBasic("partner", "partner-secret"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"customerReference":"ACME-3","lines":[{"sku":"%s","quantity":4}]}
					""".formatted(sku))
			.exchange();
		var location = placed.getResponse().getHeader("Location");
		assertThat(stockOf(sku)).isEqualTo(1);

		assertThat(this.mvc.patch()
			.uri(location + "/status")
			.with(httpBasic("admin", "admin-secret"))
			.contentType(MediaType.APPLICATION_XML)
			.content("<statusChange><status>CANCELLED</status></statusChange>")).hasStatusOk();

		assertThat(stockOf(sku)).isEqualTo(5);
		assertThat(this.mvc.patch()
			.uri(location + "/status")
			.with(httpBasic("admin", "admin-secret"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"SHIPPED\"}")).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void importsSupplierXmlFeedAsUpsert() {
		var existing = newProduct("10.00", 1);
		var created = "NEW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

		var result = this.mvc.post()
			.uri("/api/products/import")
			.with(httpBasic("admin", "admin-secret"))
			.contentType(MediaType.APPLICATION_XML)
			.accept(MediaType.APPLICATION_XML)
			.content("""
					<catalogFeed supplier="ACME" generatedAt="2026-11-25T08:00:00Z">
					  <item sku="%s"><name>Updated</name><category>test</category><price>12.00</price><stock>50</stock></item>
					  <item sku="%s"><name>Brand new</name><category>test</category><price>3.50</price><stock>7</stock></item>
					</catalogFeed>
					""".formatted(existing, created));

		assertThat(result).hasStatusOk()
			.bodyText()
			.isEqualTo("<importReport><supplier>ACME</supplier><created>1</created><updated>1</updated></importReport>");
		assertThat(this.products.findBySku(existing)).hasValueSatisfying(p -> {
			assertThat(p.name()).isEqualTo("Updated");
			assertThat(p.stock()).isEqualTo(50);
		});
		assertThat(this.products.findBySku(created)).isPresent();
	}

	@Test
	void quotesPriceInAnotherCurrency() {
		var sku = newProduct("100.00", 1);
		given(this.exchangeRateClient.latest("EUR", "USD")).willReturn(new ExchangeRates(BigDecimal.ONE, "EUR",
				LocalDate.of(2026, 11, 24), Map.of("USD", new BigDecimal("1.0512"))));

		assertThat(this.mvc.get().uri("/api/products/{sku}/price?currency=USD", sku)).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"basePrice":100.00,"baseCurrency":"EUR","price":105.12,"currency":"USD","exchangeRate":1.0512}
					""");
	}

	private String newProduct(String price, int stock) {
		var sku = "T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		var created = this.mvc.post()
			.uri("/api/products")
			.with(httpBasic("admin", "admin-secret"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"sku":"%s","name":"Test product","category":"test","price":%s,"stock":%d}
					""".formatted(sku, price, stock))
			.exchange();
		assertThat(created.getResponse().getStatus()).isEqualTo(201);
		return sku;
	}

	private int stockOf(String sku) {
		return this.products.findBySku(sku).orElseThrow().stock();
	}

}
