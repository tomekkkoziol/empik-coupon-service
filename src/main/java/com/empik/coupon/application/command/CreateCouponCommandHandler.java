package com.empik.coupon.application.command;

import com.empik.coupon.domain.exception.CouponCodeAlreadyExistsException;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponCode;
import com.empik.coupon.domain.model.CouponId;
import com.empik.coupon.domain.model.CountryCode;
import com.empik.coupon.domain.port.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
public class CreateCouponCommandHandler {

    private final CouponRepository couponRepository;
    private final Clock clock;

    public CreateCouponCommandHandler(CouponRepository couponRepository, Clock clock) {
        this.couponRepository = couponRepository;
        this.clock = clock;
    }

    public CouponId handle(CreateCouponCommand command) {
        CouponCode code = new CouponCode(command.code());
        if (couponRepository.existsByCode(code)) {
            throw new CouponCodeAlreadyExistsException(code);
        }

        Coupon coupon = Coupon.create(
                code,
                command.maxUsages(),
                new CountryCode(command.countryCode()),
                Instant.now(clock));

        return couponRepository.save(coupon).getId();
    }
}
