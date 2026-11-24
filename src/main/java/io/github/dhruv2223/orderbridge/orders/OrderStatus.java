package io.github.dhruv2223.orderbridge.orders;

public enum OrderStatus {

	PLACED, SHIPPED, DELIVERED, CANCELLED;

	boolean canTransitionTo(OrderStatus next) {
		return switch (this) {
			case PLACED -> next == SHIPPED || next == CANCELLED;
			case SHIPPED -> next == DELIVERED;
			case DELIVERED, CANCELLED -> false;
		};
	}

}
