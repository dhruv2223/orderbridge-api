package com.example.orderhub.orders;

import com.example.orderhub.catalog.Product;

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
