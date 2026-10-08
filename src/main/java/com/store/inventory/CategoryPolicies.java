package com.store.inventory;

import com.store.inventory.api.ProductCategory;
import java.time.Duration;

/**
 * Single source of truth for the category rules described in the README.
 * The exhaustive switch forces a compile error if a new category is ever added
 * without defining its policy here.
 */
final class CategoryPolicies {

    private CategoryPolicies() {
    }

    static CategoryPolicy of(ProductCategory category) {
        return switch (category) {
            case STANDARD -> new CategoryPolicy(Duration.ofMinutes(15), CategoryPolicy.NO_LIMIT);
            case PRE_ORDER -> new CategoryPolicy(Duration.ofHours(24), CategoryPolicy.NO_LIMIT);
            case FLASH_SALE -> new CategoryPolicy(Duration.ofMinutes(5), 2);
        };
    }
}
