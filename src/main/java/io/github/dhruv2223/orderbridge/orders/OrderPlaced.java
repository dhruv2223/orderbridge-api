package io.github.dhruv2223.orderbridge.orders;

import java.math.BigDecimal;
import java.util.List;

/** Application event published once an order has been stored and stock reserved. */
public record OrderPlaced(Long orderId, String customerReference, BigDecimal total, List<String> skus) {
}
