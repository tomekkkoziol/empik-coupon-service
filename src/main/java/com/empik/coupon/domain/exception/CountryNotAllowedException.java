package com.empik.coupon.domain.exception;

import com.empik.coupon.domain.model.CouponCode;
import com.empik.coupon.domain.model.CountryCode;

public class CountryNotAllowedException extends RuntimeException {

    public CountryNotAllowedException(CouponCode code, CountryCode allowed, CountryCode actual) {
        super("Coupon '" + code.value() + "' is restricted to country " + allowed.value()
                + " but request originates from " + actual.value());
    }
}
