package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.store.inventory.api.ProductCategory;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CategoryPoliciesTest {

    @Test
    void standardAllowsFifteenMinutesWithoutOrderLimit() {
        CategoryPolicy policy = CategoryPolicies.of(ProductCategory.STANDARD);
        assertEquals(Duration.ofMinutes(15), policy.reservationTtl());
        assertEquals(CategoryPolicy.NO_LIMIT, policy.orderLimit());
    }

    @Test
    void preOrderAllowsTwentyFourHoursWithoutOrderLimit() {
        CategoryPolicy policy = CategoryPolicies.of(ProductCategory.PRE_ORDER);
        assertEquals(Duration.ofHours(24), policy.reservationTtl());
        assertEquals(CategoryPolicy.NO_LIMIT, policy.orderLimit());
    }

    @Test
    void flashSaleAllowsFiveMinutesWithTwoUnitsLimit() {
        CategoryPolicy policy = CategoryPolicies.of(ProductCategory.FLASH_SALE);
        assertEquals(Duration.ofMinutes(5), policy.reservationTtl());
        assertEquals(2, policy.orderLimit());
    }
}
