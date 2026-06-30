package com.empik.coupon.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataCouponRepository extends JpaRepository<CouponJpaEntity, UUID> {

    Optional<CouponJpaEntity> findByCode(String code);

    boolean existsByCode(String code);
}
