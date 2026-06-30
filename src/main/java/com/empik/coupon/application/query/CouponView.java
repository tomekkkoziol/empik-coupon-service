package com.empik.coupon.application.query;

import java.time.Instant;

public record CouponView(
        String id,
        String code,
        Instant createdAt,
        int maxUsages,
        int currentUsages,
        String allowedCountry,
        boolean exhausted) {
}
