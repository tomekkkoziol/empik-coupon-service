package com.empik.coupon.domain.exception;

import com.empik.coupon.domain.model.CouponCode;
import com.empik.coupon.domain.model.UserId;

public class CouponAlreadyUsedException extends RuntimeException {

    public CouponAlreadyUsedException(CouponCode code, UserId userId) {
        super("User '" + userId.value() + "' has already used coupon '" + code.value() + "'");
    }
}
