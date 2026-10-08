package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.store.inventory.api.ProductCategory;
import org.junit.jupiter.api.Test;

class ProductTest {

    private final Product product = new Product(ProductCategory.STANDARD);

    @Test
    void newProductHasNoAvailableStock() {
        assertEquals(0, product.available());
    }

    @Test
    void addingStockIncreasesAvailable() {
        product.addStock(10);
        assertEquals(10, product.available());
    }

    @Test
    void addingNonPositiveStockIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> product.addStock(0));
        assertThrows(IllegalArgumentException.class, () -> product.addStock(-1));
    }
}
