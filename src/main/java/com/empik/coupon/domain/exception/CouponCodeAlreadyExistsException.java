package com.empik.coupon.domain.exception;

import com.empik.coupon.domain.model.CouponCode;

public class CouponCodeAlreadyExistsException extends RuntimeException {

    public CouponCodeAlreadyExistsException(CouponCode code) {
        super("Coupon with code '" + code.value() + "' already exists");
    }
}
