package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static final Instant EXPIRES_AT = Instant.parse("2024-01-01T00:00:00Z");

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

    @Test
    void reservingAboveTheCategoryOrderLimitIsRejected() {
        Product flashSaleProduct = new Product(ProductCategory.FLASH_SALE);
        flashSaleProduct.addStock(10);

        assertThrows(OrderLimitExceededException.class,
                () -> flashSaleProduct.reserve("ORDER-1", "SKU-1", 3, EXPIRES_AT));
    }

    @Test
    void reservingUpToTheCategoryOrderLimitSucceeds() {
        Product flashSaleProduct = new Product(ProductCategory.FLASH_SALE);
        flashSaleProduct.addStock(10);

        flashSaleProduct.reserve("ORDER-1", "SKU-1", 2, EXPIRES_AT);

        assertEquals(8, flashSaleProduct.available());
    }
}
