package com.store.inventory.demo;

import com.store.inventory.Inventory;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import java.time.Clock;

/**
 * Manual exploration tool, not part of the production API. Run with:
 * mvnw exec:java -Dexec.mainClass=com.store.inventory.demo.InventoryDemo
 */
public final class InventoryDemo {

    public static void main(String[] args) {
        InventoryService service = Inventory.create(Clock.systemUTC(),
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
    }
}
