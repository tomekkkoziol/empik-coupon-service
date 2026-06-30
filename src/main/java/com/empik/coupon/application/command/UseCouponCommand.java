package com.empik.coupon.application.command;

public record UseCouponCommand(
        String couponCode,
        String userId,
        String ipAddress) {
}
