package com.empik.coupon.domain;

import com.empik.coupon.domain.exception.CountryNotAllowedException;
import com.empik.coupon.domain.exception.CouponAlreadyUsedException;
import com.empik.coupon.domain.exception.CouponExhaustedException;
import com.empik.coupon.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class CouponTest {

    private static final CouponCode CODE = new CouponCode("WIOSNA");
    private static final CountryCode PL = new CountryCode("PL");
    private static final CountryCode DE = new CountryCode("DE");
    private static final UserId USER_1 = new UserId("user-1");
    private static final UserId USER_2 = new UserId("user-2");
    private static final Instant NOW = Instant.parse("2024-01-01T12:00:00Z");

    @Test
    void should_create_coupon_with_zero_usages() {
        Coupon coupon = Coupon.create(CODE, 10, PL, NOW);

        assertThat(coupon.getCode()).isEqualTo(CODE);
        assertThat(coupon.getMaxUsages()).isEqualTo(10);
        assertThat(coupon.getCurrentUsages()).isZero();
        assertThat(coupon.getAllowedCountry()).isEqualTo(PL);
        assertThat(coupon.getCreatedAt()).isEqualTo(NOW);
        assertThat(coupon.isExhausted()).isFalse();
        assertThat(coupon.getId()).isNotNull();
    }

    @Test
    void should_reject_non_positive_max_usages() {
        assertThatThrownBy(() -> Coupon.create(CODE, 0, PL, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxUsages must be positive");

        assertThatThrownBy(() -> Coupon.create(CODE, -5, PL, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_increment_usage_count_on_successful_use() {
        Coupon coupon = Coupon.create(CODE, 5, PL, NOW);

        coupon.use(USER_1, PL);

        assertThat(coupon.getCurrentUsages()).isEqualTo(1);
        assertThat(coupon.isExhausted()).isFalse();
    }

    @Test
    void should_mark_as_exhausted_when_max_usages_reached() {
        Coupon coupon = Coupon.create(CODE, 1, PL, NOW);

        coupon.use(USER_1, PL);

        assertThat(coupon.isExhausted()).isTrue();
        assertThat(coupon.getCurrentUsages()).isEqualTo(1);
    }

    @Test
    void should_allow_multiple_different_users_up_to_limit() {
        Coupon coupon = Coupon.create(CODE, 2, PL, NOW);

        coupon.use(USER_1, PL);
        coupon.use(USER_2, PL);

        assertThat(coupon.getCurrentUsages()).isEqualTo(2);
        assertThat(coupon.isExhausted()).isTrue();
    }

    @Test
    void should_throw_when_coupon_is_exhausted() {
        Coupon coupon = Coupon.create(CODE, 1, PL, NOW);
        coupon.use(USER_1, PL);

        assertThatThrownBy(() -> coupon.use(USER_2, PL))
                .isInstanceOf(CouponExhaustedException.class)
                .hasMessageContaining(CODE.value());
    }

    @Test
    void should_throw_when_country_does_not_match() {
        Coupon coupon = Coupon.create(CODE, 10, PL, NOW);

        assertThatThrownBy(() -> coupon.use(USER_1, DE))
                .isInstanceOf(CountryNotAllowedException.class)
                .hasMessageContaining("PL")
                .hasMessageContaining("DE");
    }

    @Test
    void should_throw_when_same_user_uses_coupon_twice() {
        Coupon coupon = Coupon.create(CODE, 10, PL, NOW);
        coupon.use(USER_1, PL);

        assertThatThrownBy(() -> coupon.use(USER_1, PL))
                .isInstanceOf(CouponAlreadyUsedException.class)
                .hasMessageContaining(USER_1.value());
    }

    @Test
    void should_check_country_before_exhaustion() {
        Coupon coupon = Coupon.create(CODE, 1, PL, NOW);
        coupon.use(USER_1, PL); // exhaust

        // Wrong country should still get CountryNotAllowedException, not CouponExhaustedException
        assertThatThrownBy(() -> coupon.use(USER_2, DE))
                .isInstanceOf(CountryNotAllowedException.class);
    }

    @Test
    void should_return_unmodifiable_set_of_users() {
        Coupon coupon = Coupon.create(CODE, 10, PL, NOW);
        coupon.use(USER_1, PL);

        assertThatThrownBy(() -> coupon.getUsedByUsers().add(USER_2))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_reconstitute_coupon_from_persistence() {
        Coupon coupon = Coupon.reconstitute(
                CouponId.generate(),
                CODE,
                NOW,
                10,
                3,
                PL,
                Set.of(USER_1, USER_2),
                5L);

        assertThat(coupon.getCurrentUsages()).isEqualTo(3);
        assertThat(coupon.getUsedByUsers()).containsExactlyInAnyOrder(USER_1, USER_2);
        assertThat(coupon.getVersion()).isEqualTo(5L);
    }
}
