package com.store.inventory.demo;

import com.store.inventory.Inventory;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Manual exploration tool, not part of the production API. Run with:
 * mvnw exec:java -Dexec.mainClass=com.store.inventory.demo.InventoryDemo
 */
public final class InventoryDemo {

    public static void main(String[] args) {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        InventoryService service = Inventory.create(clock,
                (sku, available) -> System.out.println("[ALERT] Low stock for " + sku + ": " + available + " left"));

        service.registerProduct("SKU-1", ProductCategory.STANDARD);
        service.addStock("SKU-1", 10);
        System.out.println("Available after stocking 10: " + service.available("SKU-1"));

        service.reserve("ORDER-1", "SKU-1", 7);
        System.out.println("Available after reserving 7: " + service.available("SKU-1"));

        service.confirm("ORDER-1");
        System.out.println("Available after confirming ORDER-1: " + service.available("SKU-1"));

        service.registerProduct("SKU-2", ProductCategory.FLASH_SALE);
        service.addStock("SKU-2", 1);
        try {
            service.reserve("ORDER-2", "SKU-2", 2);
        } catch (RuntimeException e) {
            System.out.println("Expected failure reserving above available stock: " + e.getMessage());
        }

        service.addStock("SKU-2", 9);
        try {
            service.reserve("ORDER-3", "SKU-2", 3);
        } catch (RuntimeException e) {
            System.out.println("Expected failure reserving above the flash sale order limit: " + e.getMessage());
        }

        service.registerProduct("SKU-3", ProductCategory.FLASH_SALE);
        service.addStock("SKU-3", 2);
        service.reserve("ORDER-4", "SKU-3", 2);
        System.out.println("Available right after reserving (not yet expired): " + service.available("SKU-3"));

        clock.advance(java.time.Duration.ofMinutes(6));
        System.out.println("Available 6 minutes later, past the 5-minute flash sale TTL: " + service.available("SKU-3"));

        try {
            service.confirm("ORDER-4");
        } catch (RuntimeException e) {
            System.out.println("Expected failure confirming an expired reservation: " + e.getMessage());
        }

        service.registerProduct("SKU-4", ProductCategory.STANDARD);
        service.addStock("SKU-4", 10);
        service.reserve("ORDER-5", "SKU-4", 4);
        service.reserve("ORDER-5", "SKU-4", 4);
        System.out.println("Available after retrying the same order twice (no double reservation): "
                + service.available("SKU-4"));

        try {
            service.reserve("ORDER-5", "SKU-4", 9);
        } catch (RuntimeException e) {
            System.out.println("Expected failure retrying the same order with different data: " + e.getMessage());
        }
    }

    /**
     * Clock whose instant can be moved forward on demand, to simulate time passing without waiting.
     */
    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(java.time.Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
