package com.store.inventory;

import com.store.inventory.api.InsufficientStockException;
import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A registered product and its stock. Owns all the invariants for a single SKU.
 */
class Product {

    private final ProductCategory category;
    private final Map<String, Reservation> activeReservations = new HashMap<>();
    private int stock;
    private int confirmed;

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

    Reservation reserve(String orderId, String sku, int quantity, Instant now) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
        CategoryPolicy policy = CategoryPolicies.of(category);
        if (quantity > policy.orderLimit()) {
            throw new OrderLimitExceededException(sku, quantity, policy.orderLimit());
        }
        releaseExpiredReservations(now);
        if (quantity > available(now)) {
            throw new InsufficientStockException(sku, quantity, available(now));
        }
        Reservation reservation = new Reservation(orderId, sku, quantity, now.plus(policy.reservationTtl()));
        activeReservations.put(orderId, reservation);
        return reservation;
    }

    void confirm(String orderId, Instant now) {
        releaseExpiredReservations(now);
        if (!(activeReservations.remove(orderId) instanceof Reservation(var o, var s, int quantity, var expiresAt))) {
            throw new IllegalStateException("No active reservation for order " + orderId);
        }
        confirmed += quantity;
    }

    int available(Instant now) {
        releaseExpiredReservations(now);
        int reserved = activeReservations.values().stream().mapToInt(Reservation::quantity).sum();
        return stock - confirmed - reserved;
    }

    private void releaseExpiredReservations(Instant now) {
        activeReservations.values().removeIf(reservation -> !reservation.expiresAt().isAfter(now));
    }
}
