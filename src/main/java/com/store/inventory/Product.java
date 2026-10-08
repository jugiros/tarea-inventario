package com.store.inventory;

import com.store.inventory.api.ProductCategory;

/**
 * A registered product and its stock. Owns all the invariants for a single SKU.
 */
class Product {

    private final ProductCategory category;
    private int stock;

    Product(ProductCategory category) {
        this.category = category;
    }

    ProductCategory category() {
        return category;
    }

    void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
        stock += quantity;
    }

    int available() {
        return stock;
    }
}
