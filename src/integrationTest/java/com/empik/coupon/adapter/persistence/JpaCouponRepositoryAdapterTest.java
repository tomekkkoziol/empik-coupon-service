package com.empik.coupon.adapter.persistence;

import com.empik.coupon.domain.exception.CouponCodeAlreadyExistsException;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponCode;
import com.empik.coupon.domain.model.CountryCode;
import com.empik.coupon.domain.model.UserId;
import com.empik.coupon.domain.port.CouponRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Transactional
class JpaCouponRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("coupon_test")
            .withUsername("coupon")
            .withPassword("coupon");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    CouponRepository couponRepository;

    private static final CouponCode WIOSNA = new CouponCode("WIOSNA");
    private static final CountryCode PL = new CountryCode("PL");
    private static final Instant NOW = Instant.parse("2024-01-01T12:00:00Z");

    @Test
    void should_save_and_find_coupon_by_code() {
        Coupon coupon = Coupon.create(WIOSNA, 100, PL, NOW);
        couponRepository.save(coupon);

        Optional<Coupon> found = couponRepository.findByCode(WIOSNA);

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo(WIOSNA);
        assertThat(found.get().getMaxUsages()).isEqualTo(100);
        assertThat(found.get().getAllowedCountry()).isEqualTo(PL);
        assertThat(found.get().getCurrentUsages()).isZero();
    }

    @Test
    void should_persist_usage_and_reload_updated_state() {
        Coupon coupon = Coupon.create(WIOSNA, 10, PL, NOW);
        Coupon saved = couponRepository.save(coupon);

        saved.use(new UserId("user-1"), PL);
        couponRepository.save(saved);

        Coupon reloaded = couponRepository.findByCode(WIOSNA).orElseThrow();
        assertThat(reloaded.getCurrentUsages()).isEqualTo(1);
        assertThat(reloaded.getUsedByUsers()).containsExactly(new UserId("user-1"));
    }

    @Test
    void should_return_false_for_non_existing_code() {
        assertThat(couponRepository.existsByCode(new CouponCode("UNKNOWN"))).isFalse();
    }

    @Test
    void should_return_true_for_existing_code() {
        couponRepository.save(Coupon.create(WIOSNA, 5, PL, NOW));

        assertThat(couponRepository.existsByCode(WIOSNA)).isTrue();
    }

    @Test
    void should_find_code_case_insensitively_due_to_value_object_normalisation() {
        couponRepository.save(Coupon.create(WIOSNA, 5, PL, NOW));

        // CouponCode normalises both sides to uppercase, so "wiosna" resolves to "WIOSNA"
        Optional<Coupon> found = couponRepository.findByCode(new CouponCode("wiosna"));
        assertThat(found).isPresent();
    }

    @Test
    void should_throw_on_duplicate_code() {
        couponRepository.save(Coupon.create(WIOSNA, 5, PL, NOW));
        Coupon duplicate = Coupon.create(WIOSNA, 10, PL, NOW);

        assertThatThrownBy(() -> {
            couponRepository.save(duplicate);
        }).isInstanceOf(CouponCodeAlreadyExistsException.class);
    }

    @Test
    void should_persist_multiple_users_for_same_coupon() {
        Coupon coupon = Coupon.create(WIOSNA, 5, PL, NOW);
        Coupon saved = couponRepository.save(coupon);

        saved.use(new UserId("user-1"), PL);
        saved.use(new UserId("user-2"), PL);
        couponRepository.save(saved);

        Coupon reloaded = couponRepository.findByCode(WIOSNA).orElseThrow();
        assertThat(reloaded.getCurrentUsages()).isEqualTo(2);
        assertThat(reloaded.getUsedByUsers())
                .containsExactlyInAnyOrder(new UserId("user-1"), new UserId("user-2"));
    }

    @Test
    void should_correctly_report_exhausted_state() {
        Coupon coupon = Coupon.create(WIOSNA, 1, PL, NOW);
        Coupon saved = couponRepository.save(coupon);

        saved.use(new UserId("user-1"), PL);
        couponRepository.save(saved);

        Coupon reloaded = couponRepository.findByCode(WIOSNA).orElseThrow();
        assertThat(reloaded.isExhausted()).isTrue();
    }
}
