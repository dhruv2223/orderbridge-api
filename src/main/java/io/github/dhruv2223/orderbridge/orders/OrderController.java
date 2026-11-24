package io.github.dhruv2223.orderbridge.orders;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/orders",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@Tag(name = "Orders", description = "Purchase orders placed by partners")
class OrderController {

	private final OrderService service;

	OrderController(OrderService service) {
		this.service = service;
	}

	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	@Operation(summary = "Place an order (JSON or XML); reserves stock atomically")
	ResponseEntity<OrderResponse> place(@Valid @RequestBody OrderRequest request) {
		var order = this.service.place(request);
		var location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(order.id())
			.toUri();
		return ResponseEntity.created(location).body(OrderResponse.from(order));
	}

	@GetMapping("/{id}")
	OrderResponse get(@PathVariable long id) {
		return OrderResponse.from(this.service.get(id));
	}

	@GetMapping
	OrderList list(@RequestParam(required = false) String customerReference) {
		return new OrderList(this.service.list(customerReference).stream().map(OrderResponse::from).toList());
	}

	@PatchMapping(path = "/{id}/status",
			consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	@Operation(summary = "Move an order to SHIPPED, DELIVERED or CANCELLED")
	OrderResponse changeStatus(@PathVariable long id, @Valid @RequestBody StatusChange change) {
		return OrderResponse.from(this.service.changeStatus(id, change.status()));
	}

}
