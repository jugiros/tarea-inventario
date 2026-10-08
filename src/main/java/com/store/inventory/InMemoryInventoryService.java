package com.store.inventory;

import com.store.inventory.api.InsufficientStockException;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import com.store.inventory.api.StockAlertListener;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of {@link InventoryService}.
 */
class InMemoryInventoryService implements InventoryService {

    private final Clock clock;
    private final StockAlertListener alertListener;
    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, String> skuByOrderId = new HashMap<>();

    InMemoryInventoryService(Clock clock, StockAlertListener alertListener) {
        this.clock = clock;
        this.alertListener = alertListener;
    }

    @Override
    public void registerProduct(String sku, ProductCategory category) {
        products.put(sku, new Product(category));
    }

    @Override
    public void addStock(String sku, int quantity) {
        Product product = products.get(sku);
        if (product == null) {
            throw new IllegalArgumentException("Product not registered: " + sku);
        }
        product.addStock(quantity);
    }

    @Override
    public Reservation reserve(String orderId, String sku, int quantity) {
        Product product = products.get(sku);
        if (product == null) {
            throw new InsufficientStockException(sku, quantity, 0);
        }
        Reservation reservation = product.reserve(orderId, sku, quantity, clock.instant());
        skuByOrderId.put(orderId, sku);
        return reservation;
    }

    @Override
    public void confirm(String orderId) {
        String sku = skuByOrderId.get(orderId);
        if (sku == null) {
            throw new IllegalStateException("No active reservation for order " + orderId);
        }
        products.get(sku).confirm(orderId, clock.instant());
        skuByOrderId.remove(orderId);
    }

    @Override
    public int available(String sku) {
        Product product = products.get(sku);
        return product == null ? 0 : product.available(clock.instant());
    }
}
