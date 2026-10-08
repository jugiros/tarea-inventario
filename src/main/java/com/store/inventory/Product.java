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
    private final Map<String, Reservation> confirmedReservations = new HashMap<>();
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

    Reservation reserve(String orderId, String sku, int quantity, Instant now) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
        releaseExpiredReservations(now);

        Reservation retried = retryOf(orderId, sku, quantity);
        if (retried != null) {
            return retried;
        }

        CategoryPolicy policy = CategoryPolicies.of(category);
        if (quantity > policy.orderLimit()) {
            throw new OrderLimitExceededException(sku, quantity, policy.orderLimit());
        }
        if (quantity > available(now)) {
            throw new InsufficientStockException(sku, quantity, available(now));
        }
        Reservation reservation = new Reservation(orderId, sku, quantity, now.plus(policy.reservationTtl()));
        activeReservations.put(orderId, reservation);
        return reservation;
    }

    /**
     * Retries of the same order (e.g. the mobile app resending a slow request) must not double-reserve
     * or fail just because the order was already handled. Returns the existing reservation when the
     * retry matches it, {@code null} when this is a brand new order, or fails when the retry conflicts
     * with a previous call for the same order.
     */
    private Reservation retryOf(String orderId, String sku, int quantity) {
        Reservation existing = activeReservations.get(orderId);
        if (existing == null) {
            existing = confirmedReservations.get(orderId);
        }
        if (existing == null) {
            return null;
        }
        if (existing.sku().equals(sku) && existing.quantity() == quantity) {
            return existing;
        }
        throw new IllegalStateException("Order " + orderId + " was already placed with different data");
    }

    void confirm(String orderId, Instant now) {
        releaseExpiredReservations(now);
        if (!(activeReservations.remove(orderId) instanceof Reservation reservation)) {
            throw new IllegalStateException("No active reservation for order " + orderId);
        }
        confirmedReservations.put(orderId, reservation);
    }

    int available(Instant now) {
        releaseExpiredReservations(now);
        int reserved = activeReservations.values().stream().mapToInt(Reservation::quantity).sum();
        int confirmed = confirmedReservations.values().stream().mapToInt(Reservation::quantity).sum();
        return stock - confirmed - reserved;
    }

    private void releaseExpiredReservations(Instant now) {
        activeReservations.values().removeIf(reservation -> !reservation.expiresAt().isAfter(now));
    }
}
