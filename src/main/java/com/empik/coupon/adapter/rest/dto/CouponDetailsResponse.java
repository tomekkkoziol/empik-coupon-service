package com.empik.coupon.adapter.rest.dto;

import com.empik.coupon.application.query.CouponView;

import java.time.Instant;

public record CouponDetailsResponse(
        String id,
        String code,
        Instant createdAt,
        int maxUsages,
        int currentUsages,
        String allowedCountry,
        boolean exhausted) {

    public static CouponDetailsResponse from(CouponView view) {
        return new CouponDetailsResponse(
                view.id(),
                view.code(),
                view.createdAt(),
                view.maxUsages(),
                view.currentUsages(),
                view.allowedCountry(),
                view.exhausted());
    }
}
