package com.empik.coupon.application.query;

import com.empik.coupon.domain.exception.CouponNotFoundException;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponCode;
import com.empik.coupon.domain.port.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetCouponQueryHandler {

    private final CouponRepository couponRepository;

    public GetCouponQueryHandler(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public CouponView handle(GetCouponQuery query) {
        CouponCode code = new CouponCode(query.couponCode());
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new CouponNotFoundException(code));
        return toView(coupon);
    }

    private CouponView toView(Coupon coupon) {
        return new CouponView(
                coupon.getId().toString(),
                coupon.getCode().value(),
                coupon.getCreatedAt(),
                coupon.getMaxUsages(),
                coupon.getCurrentUsages(),
                coupon.getAllowedCountry().value(),
                coupon.isExhausted());
    }
}
