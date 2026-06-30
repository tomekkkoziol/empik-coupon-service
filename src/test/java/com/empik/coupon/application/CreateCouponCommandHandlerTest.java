package com.empik.coupon.application;

import com.empik.coupon.application.command.CreateCouponCommand;
import com.empik.coupon.application.command.CreateCouponCommandHandler;
import com.empik.coupon.domain.exception.CouponCodeAlreadyExistsException;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponCode;
import com.empik.coupon.domain.model.CouponId;
import com.empik.coupon.domain.model.CountryCode;
import com.empik.coupon.domain.port.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCouponCommandHandlerTest {

    @Mock
    CouponRepository couponRepository;

    CreateCouponCommandHandler handler;

    private static final Instant FIXED_NOW = Instant.parse("2024-01-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        handler = new CreateCouponCommandHandler(couponRepository, fixedClock);
    }

    @Test
    void should_create_coupon_and_return_id() {
        given(couponRepository.existsByCode(new CouponCode("SUMMER20"))).willReturn(false);
        Coupon saved = Coupon.create(new CouponCode("SUMMER20"), 100, new CountryCode("PL"), FIXED_NOW);
        given(couponRepository.save(any(Coupon.class))).willReturn(saved);

        CouponId result = handler.handle(new CreateCouponCommand("SUMMER20", 100, "PL"));

        assertThat(result).isNotNull();
        then(couponRepository).should().save(any(Coupon.class));
    }

    @Test
    void should_normalise_code_to_uppercase_before_checking_existence() {
        given(couponRepository.existsByCode(new CouponCode("summer20"))).willReturn(false);
        Coupon saved = Coupon.create(new CouponCode("SUMMER20"), 50, new CountryCode("DE"), FIXED_NOW);
        given(couponRepository.save(any(Coupon.class))).willReturn(saved);

        handler.handle(new CreateCouponCommand("summer20", 50, "DE"));

        then(couponRepository).should().existsByCode(new CouponCode("SUMMER20"));
    }

    @Test
    void should_throw_when_code_already_exists() {
        given(couponRepository.existsByCode(new CouponCode("DUPLICATE"))).willReturn(true);

        assertThatThrownBy(() -> handler.handle(new CreateCouponCommand("DUPLICATE", 10, "PL")))
                .isInstanceOf(CouponCodeAlreadyExistsException.class)
                .hasMessageContaining("DUPLICATE");

        then(couponRepository).should(never()).save(any());
    }

    @Test
    void should_reject_non_positive_max_usages() {
        given(couponRepository.existsByCode(any())).willReturn(false);

        assertThatThrownBy(() -> handler.handle(new CreateCouponCommand("VALID", 0, "PL")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
