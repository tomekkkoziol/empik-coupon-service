package com.empik.coupon.application.command;

public record CreateCouponCommand(
        String code,
        int maxUsages,
        String countryCode) {
}
