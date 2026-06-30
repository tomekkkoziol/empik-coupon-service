package com.empik.coupon.adapter.persistence;

import com.empik.coupon.domain.exception.CouponCodeAlreadyExistsException;
import com.empik.coupon.domain.model.*;
import com.empik.coupon.domain.port.CouponRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class JpaCouponRepositoryAdapter implements CouponRepository {

    private final SpringDataCouponRepository jpaRepository;

    JpaCouponRepositoryAdapter(SpringDataCouponRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        try {
            CouponJpaEntity entity = toEntity(coupon);
            CouponJpaEntity saved = jpaRepository.save(entity);
            return toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new CouponCodeAlreadyExistsException(coupon.getCode());
        }
    }

    @Override
    public Optional<Coupon> findByCode(CouponCode code) {
        return jpaRepository.findByCode(code.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(CouponCode code) {
        return jpaRepository.existsByCode(code.value());
    }

    private Coupon toDomain(CouponJpaEntity entity) {
        Set<UserId> usedByUsers = entity.getUsedByUserIds().stream()
                .map(UserId::new)
                .collect(Collectors.toSet());

        return Coupon.reconstitute(
                new CouponId(entity.getId()),
                new CouponCode(entity.getCode()),
                entity.getCreatedAt(),
                entity.getMaxUsages(),
                entity.getCurrentUsages(),
                new CountryCode(entity.getAllowedCountry()),
                usedByUsers,
                entity.getVersion());
    }

    private CouponJpaEntity toEntity(Coupon coupon) {
        CouponJpaEntity entity = new CouponJpaEntity();
        entity.setId(coupon.getId().value());
        entity.setCode(coupon.getCode().value());
        entity.setCreatedAt(coupon.getCreatedAt());
        entity.setMaxUsages(coupon.getMaxUsages());
        entity.setCurrentUsages(coupon.getCurrentUsages());
        entity.setAllowedCountry(coupon.getAllowedCountry().value());
        entity.setUsedByUserIds(coupon.getUsedByUsers().stream()
                .map(UserId::value)
                .collect(Collectors.toSet()));
        entity.setVersion(coupon.getVersion());
        return entity;
    }
}
