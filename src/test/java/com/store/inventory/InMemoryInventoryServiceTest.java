package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.store.inventory.api.InsufficientStockException;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
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
    void reservingNonPositiveQuantityIsRejectedEvenForAnUnknownSku() {
        assertThrows(IllegalArgumentException.class, () -> service.reserve("ORDER-1", "UNKNOWN-SKU", -5));
    }

    @Test
    void reservingAnUnknownSkuIsTreatedAsNoStockAvailable() {
        assertThrows(InsufficientStockException.class, () -> service.reserve("ORDER-1", "UNKNOWN-SKU", 1));
    }

    @Test
    void availableForAnUnknownSkuIsZero() {
        assertEquals(0, service.available("UNKNOWN-SKU"));
    }

    @Test
    void addingStockToAnUnregisteredProductIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addStock("UNKNOWN-SKU", 10));
    }

    @Test
    void confirmingAnOrderThatWasNeverReservedIsRejected() {
        assertThrows(IllegalStateException.class, () -> service.confirm("NEVER-RESERVED"));
    }

    @Test
    void confirmingTheSameOrderTwiceIsRejectedTheSecondTime() {
        service.reserve("ORDER-1", "SKU-1", 3);
        service.confirm("ORDER-1");

        assertThrows(IllegalStateException.class, () -> service.confirm("ORDER-1"));
    }

    @Test
    void preOrderCategoryAllowsLargeQuantitiesOverALongerWindow() {
        service.registerProduct("SKU-PREORDER", ProductCategory.PRE_ORDER);
        service.addStock("SKU-PREORDER", 500);

        service.reserve("ORDER-1", "SKU-PREORDER", 300);

        assertEquals(200, service.available("SKU-PREORDER"));
    }

    @Test
    void concurrentReservationsNeverOversellStock() throws Exception {
        int stock = 100;
        int attempts = 300;
        service.registerProduct("SKU-CONCURRENT", ProductCategory.STANDARD);
        service.addStock("SKU-CONCURRENT", stock);

        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();

        List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            String orderId = "CONCURRENT-ORDER-" + i;
            tasks.add(pool.submit(() -> {
                start.await();
                try {
                    service.reserve(orderId, "SKU-CONCURRENT", 1);
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException expectedOnceSoldOut) {
                    // expected once the stock runs out
                }
                return null;
            }));
        }

        start.countDown();
        for (Future<?> task : tasks) {
            task.get();
        }
        pool.shutdown();

        assertEquals(stock, succeeded.get());
        assertEquals(0, service.available("SKU-CONCURRENT"));
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
