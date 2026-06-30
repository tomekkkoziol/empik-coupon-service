package com.empik.coupon.application.command;

import com.empik.coupon.domain.exception.CouponNotFoundException;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponCode;
import com.empik.coupon.domain.model.CountryCode;
import com.empik.coupon.domain.model.UserId;
import com.empik.coupon.domain.port.CouponRepository;
import com.empik.coupon.domain.port.GeoLocationPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UseCouponCommandHandler {

    private final CouponRepository couponRepository;
    private final GeoLocationPort geoLocationPort;

    public UseCouponCommandHandler(CouponRepository couponRepository, GeoLocationPort geoLocationPort) {
        this.couponRepository = couponRepository;
        this.geoLocationPort = geoLocationPort;
    }

    public void handle(UseCouponCommand command) {
        CouponCode code = new CouponCode(command.couponCode());

        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new CouponNotFoundException(code));

        CountryCode userCountry = geoLocationPort.resolveCountry(command.ipAddress());
        coupon.use(new UserId(command.userId()), userCountry);

        couponRepository.save(coupon);
    }
}
