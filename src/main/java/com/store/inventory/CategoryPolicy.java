package com.store.inventory;

import java.time.Duration;

/**
 * Business rules applied to a reservation based on the product's category.
 */
record CategoryPolicy(Duration reservationTtl, int orderLimit) {

    static final int NO_LIMIT = Integer.MAX_VALUE;
}
