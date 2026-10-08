package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private final Product product = new Product(ProductCategory.STANDARD);

    @Test
    void newProductHasNoAvailableStock() {
        assertEquals(0, product.available(NOW));
    }

    @Test
    void addingStockIncreasesAvailable() {
        product.addStock(10);
        assertEquals(10, product.available(NOW));
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
                () -> flashSaleProduct.reserve("ORDER-1", "SKU-1", 3, NOW));
    }

    @Test
    void reservingUpToTheCategoryOrderLimitSucceeds() {
        Product flashSaleProduct = new Product(ProductCategory.FLASH_SALE);
        flashSaleProduct.addStock(10);

        flashSaleProduct.reserve("ORDER-1", "SKU-1", 2, NOW);

        assertEquals(8, flashSaleProduct.available(NOW));
    }

    @Test
    void reservationStillBlocksStockBeforeItExpires() {
        product.addStock(10);
        product.reserve("ORDER-1", "SKU-1", 5, NOW);

        Instant beforeExpiry = NOW.plusSeconds(14 * 60);

        assertEquals(5, product.available(beforeExpiry));
    }

    @Test
    void reservationReleasesStockOnceItExpires() {
        product.addStock(10);
        product.reserve("ORDER-1", "SKU-1", 5, NOW);

        Instant afterExpiry = NOW.plusSeconds(16 * 60);

        assertEquals(10, product.available(afterExpiry));
    }

    @Test
    void confirmingAnExpiredReservationIsRejected() {
        product.addStock(10);
        product.reserve("ORDER-1", "SKU-1", 5, NOW);

        Instant afterExpiry = NOW.plusSeconds(16 * 60);

        assertThrows(IllegalStateException.class, () -> product.confirm("ORDER-1", afterExpiry));
    }

    @Test
    void expiredReservationCanBeReplacedByANewOrder() {
        product.addStock(5);
        product.reserve("ORDER-1", "SKU-1", 5, NOW);

        Instant afterExpiry = NOW.plusSeconds(16 * 60);

        product.reserve("ORDER-2", "SKU-1", 5, afterExpiry);

        assertEquals(0, product.available(afterExpiry));
    }

    @Test
    void retryingTheSameActiveReservationReturnsTheSameOneWithoutDoubleCounting() {
        product.addStock(10);
        Reservation first = product.reserve("ORDER-1", "SKU-1", 5, NOW);

        Reservation retry = product.reserve("ORDER-1", "SKU-1", 5, NOW);

        assertEquals(first, retry);
        assertEquals(5, product.available(NOW));
    }

    @Test
    void retryingWithDifferentQuantityForTheSameOrderIsRejected() {
        product.addStock(10);
        product.reserve("ORDER-1", "SKU-1", 5, NOW);

        assertThrows(IllegalStateException.class, () -> product.reserve("ORDER-1", "SKU-1", 6, NOW));
    }

    @Test
    void retryingAfterTheOrderWasAlreadyConfirmedReturnsTheConfirmedReservation() {
        product.addStock(10);
        Reservation first = product.reserve("ORDER-1", "SKU-1", 5, NOW);
        product.confirm("ORDER-1", NOW);

        Reservation retry = product.reserve("ORDER-1", "SKU-1", 5, NOW);

        assertEquals(first, retry);
        assertEquals(5, product.available(NOW));
    }

    @Test
    void retryingWithDifferentQuantityAfterConfirmationIsRejected() {
        product.addStock(10);
        product.reserve("ORDER-1", "SKU-1", 5, NOW);
        product.confirm("ORDER-1", NOW);

        assertThrows(IllegalStateException.class, () -> product.reserve("ORDER-1", "SKU-1", 6, NOW));
    }

    @Test
    void reachingTheLowStockThresholdSignalsOnce() {
        product.addStock(10);
        product.reserve("ORDER-1", "SKU-1", 5, NOW);

        assertTrue(product.shouldAlertLowStock(NOW));
        assertFalse(product.shouldAlertLowStock(NOW));
    }

    @Test
    void stayingAboveTheLowStockThresholdDoesNotSignal() {
        product.addStock(10);
        product.reserve("ORDER-1", "SKU-1", 2, NOW);

        assertFalse(product.shouldAlertLowStock(NOW));
    }

    @Test
    void restockingAllowsTheAlertToSignalAgainOnTheNextDrop() {
        product.addStock(10);
        product.reserve("ORDER-1", "SKU-1", 5, NOW); // 5 left, at the threshold
        assertTrue(product.shouldAlertLowStock(NOW));

        product.addStock(10); // 15 left, well above the threshold
        product.reserve("ORDER-2", "SKU-1", 10, NOW); // 5 left again

        assertTrue(product.shouldAlertLowStock(NOW));
    }
}
