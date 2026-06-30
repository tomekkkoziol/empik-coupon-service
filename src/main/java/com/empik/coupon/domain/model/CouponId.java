package com.empik.coupon.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CouponId(UUID value) {

    public CouponId {
        Objects.requireNonNull(value, "CouponId value cannot be null");
    }

    public static CouponId generate() {
        return new CouponId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
