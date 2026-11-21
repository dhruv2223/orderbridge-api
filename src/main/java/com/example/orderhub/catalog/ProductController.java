package com.example.orderhub.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Every endpoint produces and consumes both JSON and XML; the representation is chosen
 * from the {@code Accept} / {@code Content-Type} headers (or {@code ?format=xml}).
 */
@RestController
@RequestMapping(path = "/api/products",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@Tag(name = "Products", description = "Product catalog")
class ProductController {

	private final ProductService service;

	ProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List products, optionally filtered by category")
	ProductPage list(@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ProductPage.from(this.service.list(category, page, size));
	}

	@GetMapping("/{sku}")
	ProductResponse get(@PathVariable String sku) {
		return ProductResponse.from(this.service.get(sku));
	}

	@GetMapping("/{sku}/price")
	@Operation(summary = "Price a product in another currency using live exchange rates")
	PriceQuote price(@PathVariable String sku,
			@RequestParam @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO 4217 code like USD") String currency) {
		return this.service.quote(sku, currency);
	}

	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
		var product = this.service.create(request);
		var location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{sku}")
			.buildAndExpand(product.sku())
			.toUri();
		return ResponseEntity.created(location).body(ProductResponse.from(product));
	}

	@PutMapping(path = "/{sku}", consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	ProductResponse replace(@PathVariable String sku, @Valid @RequestBody ProductRequest request) {
		return ProductResponse.from(this.service.replace(sku, request));
	}

	@DeleteMapping("/{sku}")
	ResponseEntity<Void> delete(@PathVariable String sku) {
		this.service.delete(sku);
		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/import", consumes = MediaType.APPLICATION_XML_VALUE)
	@Operation(summary = "Bulk upsert products from a supplier XML feed")
	ImportReport importFeed(@Valid @RequestBody CatalogFeed feed) {
		return this.service.importFeed(feed);
	}

}
