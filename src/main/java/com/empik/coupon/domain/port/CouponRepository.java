package com.empik.coupon.domain.port;

import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponCode;

import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findByCode(CouponCode code);

    boolean existsByCode(CouponCode code);
}
