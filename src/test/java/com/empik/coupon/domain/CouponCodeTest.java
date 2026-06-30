package com.empik.coupon.domain;

import com.empik.coupon.domain.model.CouponCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class CouponCodeTest {

    @Test
    void should_normalise_to_uppercase() {
        assertThat(new CouponCode("wiosna").value()).isEqualTo("WIOSNA");
        assertThat(new CouponCode("Wiosna").value()).isEqualTo("WIOSNA");
        assertThat(new CouponCode("WIOSNA").value()).isEqualTo("WIOSNA");
    }

    @Test
    void should_treat_lowercase_and_uppercase_as_equal() {
        CouponCode lower = new CouponCode("wiosna");
        CouponCode upper = new CouponCode("WIOSNA");
        CouponCode mixed = new CouponCode("Wiosna");

        assertThat(lower).isEqualTo(upper);
        assertThat(lower).isEqualTo(mixed);
        assertThat(lower.hashCode()).isEqualTo(upper.hashCode());
    }

    @Test
    void should_strip_surrounding_whitespace() {
        assertThat(new CouponCode("  WIOSNA  ").value()).isEqualTo("WIOSNA");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void should_reject_blank_code(String blank) {
        assertThatThrownBy(() -> new CouponCode(blank))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void should_reject_null_code() {
        assertThatThrownBy(() -> new CouponCode(null))
                .isInstanceOf(NullPointerException.class);
    }
}
