package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
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

    @Test
    void lowStockAlertIsSentOnceUntilRestocked() {
        List<String> alerts = new ArrayList<>();
        InventoryService watched = Inventory.create(Clock.systemUTC(),
                (sku, available) -> alerts.add(sku + ":" + available));
        watched.registerProduct("SKU-X", ProductCategory.STANDARD);
        watched.addStock("SKU-X", 10);

        watched.reserve("ORDER-1", "SKU-X", 5); // leaves 5, at the threshold
        watched.reserve("ORDER-2", "SKU-X", 1); // leaves 4, already alerted

        assertEquals(List.of("SKU-X:5"), alerts);
    }

    @Test
    void aFailingAlertListenerDoesNotPreventTheReservation() {
        InventoryService watched = Inventory.create(Clock.systemUTC(),
                (sku, available) -> {
                    throw new RuntimeException("notification channel is down");
                });
        watched.registerProduct("SKU-Y", ProductCategory.STANDARD);
        watched.addStock("SKU-Y", 5);

        watched.reserve("ORDER-1", "SKU-Y", 5);

        assertEquals(0, watched.available("SKU-Y"));
    }
}
