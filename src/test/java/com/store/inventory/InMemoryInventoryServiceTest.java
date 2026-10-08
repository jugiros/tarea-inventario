package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryInventoryServiceTest {

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = Inventory.create(Clock.systemUTC(), (sku, available) -> { });
        service.registerProduct("SKU-1", ProductCategory.STANDARD);
        service.registerProduct("SKU-2", ProductCategory.STANDARD);
        service.addStock("SKU-1", 10);
        service.addStock("SKU-2", 10);
    }

    @Test
    void retryingTheSameOrderForADifferentSkuIsRejected() {
        service.reserve("ORDER-1", "SKU-1", 3);

        assertThrows(IllegalStateException.class, () -> service.reserve("ORDER-1", "SKU-2", 3));
    }
}
