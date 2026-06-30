package com.empik.coupon.application;

import com.empik.coupon.application.command.UseCouponCommand;
import com.empik.coupon.application.command.UseCouponCommandHandler;
import com.empik.coupon.domain.exception.CountryNotAllowedException;
import com.empik.coupon.domain.exception.CouponAlreadyUsedException;
import com.empik.coupon.domain.exception.CouponExhaustedException;
import com.empik.coupon.domain.exception.CouponNotFoundException;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponCode;
import com.empik.coupon.domain.model.CountryCode;
import com.empik.coupon.domain.port.CouponRepository;
import com.empik.coupon.domain.port.GeoLocationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UseCouponCommandHandlerTest {

    @Mock
    CouponRepository couponRepository;

    @Mock
    GeoLocationPort geoLocationPort;

    UseCouponCommandHandler handler;

    private static final CountryCode PL = new CountryCode("PL");
    private static final CountryCode DE = new CountryCode("DE");
    private static final Instant NOW = Instant.parse("2024-01-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        handler = new UseCouponCommandHandler(couponRepository, geoLocationPort);
    }

    @Test
    void should_use_coupon_successfully() {
        Coupon coupon = Coupon.create(new CouponCode("SPRING"), 10, PL, NOW);
        given(couponRepository.findByCode(new CouponCode("SPRING"))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("5.10.10.10")).willReturn(PL);
        given(couponRepository.save(any(Coupon.class))).willReturn(coupon);

        handler.handle(new UseCouponCommand("SPRING", "user-1", "5.10.10.10"));

        assertThat(coupon.getCurrentUsages()).isEqualTo(1);
        then(couponRepository).should().save(coupon);
    }

    @Test
    void should_throw_when_coupon_not_found() {
        given(couponRepository.findByCode(new CouponCode("GHOST"))).willReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new UseCouponCommand("GHOST", "user-1", "5.10.10.10")))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessageContaining("GHOST");

        then(couponRepository).should(never()).save(any());
    }

    @Test
    void should_throw_when_country_not_allowed() {
        Coupon coupon = Coupon.create(new CouponCode("SPRING"), 10, PL, NOW);
        given(couponRepository.findByCode(new CouponCode("SPRING"))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("84.10.10.10")).willReturn(DE);

        assertThatThrownBy(() -> handler.handle(new UseCouponCommand("SPRING", "user-1", "84.10.10.10")))
                .isInstanceOf(CountryNotAllowedException.class);

        then(couponRepository).should(never()).save(any());
    }

    @Test
    void should_throw_when_coupon_exhausted() {
        Coupon coupon = Coupon.create(new CouponCode("SPRING"), 1, PL, NOW);
        coupon.use(new com.empik.coupon.domain.model.UserId("existing-user"), PL);
        given(couponRepository.findByCode(new CouponCode("SPRING"))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("5.10.10.10")).willReturn(PL);

        assertThatThrownBy(() -> handler.handle(new UseCouponCommand("SPRING", "new-user", "5.10.10.10")))
                .isInstanceOf(CouponExhaustedException.class);
    }

    @Test
    void should_throw_when_user_already_used_coupon() {
        Coupon coupon = Coupon.create(new CouponCode("SPRING"), 10, PL, NOW);
        coupon.use(new com.empik.coupon.domain.model.UserId("user-1"), PL);
        given(couponRepository.findByCode(new CouponCode("SPRING"))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("5.10.10.10")).willReturn(PL);

        assertThatThrownBy(() -> handler.handle(new UseCouponCommand("SPRING", "user-1", "5.10.10.10")))
                .isInstanceOf(CouponAlreadyUsedException.class)
                .hasMessageContaining("user-1");
    }

    @Test
    void should_normalise_coupon_code_to_uppercase() {
        Coupon coupon = Coupon.create(new CouponCode("SPRING"), 10, PL, NOW);
        given(couponRepository.findByCode(new CouponCode("spring"))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry(any())).willReturn(PL);
        given(couponRepository.save(any())).willReturn(coupon);

        handler.handle(new UseCouponCommand("spring", "user-1", "5.10.10.10"));

        then(couponRepository).should().findByCode(new CouponCode("SPRING"));
    }
}
