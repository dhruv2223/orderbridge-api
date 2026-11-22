package com.example.orderhub.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Aggregate root: the order and its lines are loaded and saved together (one-to-many).
 */
@Table("purchase_order")
public record PurchaseOrder(@Id Long id, String customerReference, OrderStatus status, BigDecimal total,
		Instant createdAt, @MappedCollection(idColumn = "order_id", keyColumn = "line_no") List<OrderLine> lines,
		@Version Integer version) {

	static PurchaseOrder place(String customerReference, List<OrderLine> lines, Instant now) {
		var total = lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new PurchaseOrder(null, customerReference, OrderStatus.PLACED, total, now, List.copyOf(lines), null);
	}

	PurchaseOrder withStatus(OrderStatus next) {
		if (!this.status.canTransitionTo(next)) {
			throw new IllegalStateException("Cannot move order " + this.id + " from " + this.status + " to " + next);
		}
		return new PurchaseOrder(this.id, this.customerReference, next, this.total, this.createdAt, this.lines,
				this.version);
	}

}
