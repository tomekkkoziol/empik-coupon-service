package com.empik.coupon.domain.model;

import java.util.Locale;
import java.util.Objects;

public record CouponCode(String value) {

    public CouponCode {
        Objects.requireNonNull(value, "Coupon code cannot be null");
        value = value.strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Coupon code cannot be blank");
        }
        value = value.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
