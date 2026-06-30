package com.empik.coupon.domain.exception;

import com.empik.coupon.domain.model.CouponCode;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(CouponCode code) {
        super("Coupon not found: " + code.value());
    }
}
