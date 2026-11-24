package io.github.dhruv2223.orderbridge.orders;

import java.util.List;

public class OrderRejectedException extends RuntimeException {

	private final List<String> violations;

	OrderRejectedException(List<String> violations) {
		super("Order cannot be fulfilled");
		this.violations = List.copyOf(violations);
	}

	public List<String> getViolations() {
		return this.violations;
	}

}
