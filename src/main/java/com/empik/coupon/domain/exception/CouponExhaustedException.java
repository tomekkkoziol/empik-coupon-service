package com.empik.coupon.domain.exception;

import com.empik.coupon.domain.model.CouponCode;

public class CouponExhaustedException extends RuntimeException {

    public CouponExhaustedException(CouponCode code) {
        super("Coupon '" + code.value() + "' has reached its maximum usage limit");
    }
}
