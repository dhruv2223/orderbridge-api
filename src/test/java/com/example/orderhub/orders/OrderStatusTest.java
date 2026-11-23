package com.example.orderhub.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class OrderStatusTest {

	@ParameterizedTest
	@CsvSource({ "PLACED,SHIPPED,true", "PLACED,CANCELLED,true", "PLACED,DELIVERED,false", "SHIPPED,DELIVERED,true",
			"SHIPPED,CANCELLED,false", "DELIVERED,CANCELLED,false", "CANCELLED,PLACED,false" })
	void allowsOnlyForwardTransitions(OrderStatus from, OrderStatus to, boolean allowed) {
		assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
	}

	@Test
	void placeComputesTotalFromLines() {
		var order = PurchaseOrder.place("ACME", List.of(new OrderLine("A", 3, new BigDecimal("10.50")),
				new OrderLine("B", 1, new BigDecimal("4.00"))), Instant.EPOCH);

		assertThat(order.total()).isEqualByComparingTo("35.50");
		assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
	}

	@Test
	void withStatusRejectsIllegalTransition() {
		var delivered = new PurchaseOrder(1L, "ACME", OrderStatus.DELIVERED, BigDecimal.ONE, Instant.EPOCH, List.of(),
				0);

		assertThatIllegalStateException().isThrownBy(() -> delivered.withStatus(OrderStatus.CANCELLED))
			.withMessageContaining("DELIVERED to CANCELLED");
	}

}
