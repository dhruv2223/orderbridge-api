package io.github.dhruv2223.orderbridge.orders;

import io.github.dhruv2223.orderbridge.catalog.Product;

/**
 * Result of checking one requested line against inventory. Sealed so that callers'
 * {@code switch} statements are checked for exhaustiveness by the compiler.
 */
sealed interface StockCheck {

	record Available(Product product, int quantity) implements StockCheck {
	}

	record Insufficient(String sku, int requested, int available) implements StockCheck {
	}

	record UnknownSku(String sku) implements StockCheck {
	}

}
