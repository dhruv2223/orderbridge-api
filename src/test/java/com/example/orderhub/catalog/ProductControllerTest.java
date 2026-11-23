package com.example.orderhub.catalog;

import java.math.BigDecimal;
import java.util.List;

import com.example.orderhub.security.SecurityConfiguration;
import com.example.orderhub.web.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Web-layer slice test: content negotiation, validation, error format and security rules.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfiguration.class)
class ProductControllerTest {

	private static final Product KEYBOARD = new Product(1L, "KB-100", "Mechanical Keyboard", null, "peripherals",
			new BigDecimal("89.90"), 40, 0);

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	ProductService service;

	@Test
	void listDefaultsToJson() {
		given(this.service.list(null, 0, 20)).willReturn(new PageImpl<>(List.of(KEYBOARD), PageRequest.of(0, 20), 1));

		assertThat(this.mvc.get().uri("/api/products")).hasStatusOk()
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
			.bodyJson()
			.isLenientlyEqualTo("""
					{"items":[{"sku":"KB-100","name":"Mechanical Keyboard","price":89.90,"currency":"EUR","stock":40}],
					 "page":0,"size":20,"totalElements":1,"totalPages":1}
					""");
	}

	@Test
	void listRendersXmlWithAttributesWhenRequested() {
		given(this.service.list(null, 0, 20)).willReturn(new PageImpl<>(List.of(KEYBOARD), PageRequest.of(0, 20), 1));

		var result = this.mvc.get().uri("/api/products").accept(MediaType.APPLICATION_XML);

		assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.APPLICATION_XML);
		assertThat(result).bodyText()
			.contains("<products page=\"0\" size=\"20\" totalElements=\"1\" totalPages=\"1\">")
			.contains("<product sku=\"KB-100\"><name>Mechanical Keyboard</name>")
			.doesNotContain("<description");
	}

	@Test
	void formatParameterSelectsXml() {
		given(this.service.get("KB-100")).willReturn(KEYBOARD);

		assertThat(this.mvc.get().uri("/api/products/KB-100?format=xml")).hasStatusOk()
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_XML);
	}

	@Test
	void notFoundIsProblemJsonByDefault() {
		given(this.service.get("NOPE")).willThrow(new NotFoundException("Product", "NOPE"));

		assertThat(this.mvc.get().uri("/api/products/NOPE")).hasStatus(HttpStatus.NOT_FOUND)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.isLenientlyEqualTo("""
					{"status":404,"title":"Resource not found","detail":"Product 'NOPE' was not found",
					 "type":"https://order-hub.example.com/problems/not-found"}
					""");
	}

	@Test
	void notFoundIsProblemXmlForXmlClients() {
		given(this.service.get("NOPE")).willThrow(new NotFoundException("Product", "NOPE"));

		var result = this.mvc.get().uri("/api/products/NOPE").accept(MediaType.APPLICATION_XML);

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_XML);
		assertThat(result).bodyText()
			.contains("<problem xmlns=\"urn:ietf:rfc:7807\">")
			.contains("<status>404</status>");
	}

	@Test
	void createAcceptsXmlBodyAndReturnsLocation() {
		given(this.service.create(any())).willReturn(KEYBOARD);

		var result = this.mvc.post()
			.uri("/api/products")
			.with(httpBasic("admin", "admin-secret"))
			.contentType(MediaType.APPLICATION_XML)
			.accept(MediaType.APPLICATION_JSON)
			.content("""
					<product>
					  <sku>KB-100</sku><name>Mechanical Keyboard</name><category>peripherals</category>
					  <price>89.90</price><stock>40</stock>
					</product>
					""");

		assertThat(result).hasStatus(HttpStatus.CREATED)
			.headers()
			.hasValue("Location", "http://localhost/api/products/KB-100");
	}

	@Test
	void createReportsEveryInvalidField() {
		var result = this.mvc.post()
			.uri("/api/products")
			.with(httpBasic("admin", "admin-secret"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"sku":"bad sku","name":"","category":"x","price":-1,"stock":1}
					""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactlyInAnyOrder("sku", "name", "price");
	}

	@Test
	void writesRequireAdminRole() {
		var body = """
				{"sku":"KB-100","name":"x","category":"x","price":1,"stock":1}
				""";
		assertThat(this.mvc.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
			.hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(this.mvc.post()
			.uri("/api/products")
			.with(httpBasic("partner", "partner-secret"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void importRejectsJsonFeeds() {
		assertThat(this.mvc.post()
			.uri("/api/products/import")
			.with(httpBasic("admin", "admin-secret"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	@Test
	void priceValidatesCurrencyCode() {
		assertThat(this.mvc.get().uri("/api/products/KB-100/price?currency=usd")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("currency");
	}

}
