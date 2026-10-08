package com.store.inventory;

/**
 * Shared quantity validation, used regardless of whether the product being validated exists.
 */
final class Quantities {

    private Quantities() {
    }

    static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
    }
}
