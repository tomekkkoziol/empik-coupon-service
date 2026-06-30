package com.empik.coupon.domain.model;

import com.empik.coupon.domain.exception.CountryNotAllowedException;
import com.empik.coupon.domain.exception.CouponAlreadyUsedException;
import com.empik.coupon.domain.exception.CouponExhaustedException;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Coupon {

    private final CouponId id;
    private final CouponCode code;
    private final Instant createdAt;
    private final int maxUsages;
    private int currentUsages;
    private final CountryCode allowedCountry;
    private final Set<UserId> usedByUsers;
    private final long version;

    private Coupon(
            CouponId id,
            CouponCode code,
            Instant createdAt,
            int maxUsages,
            int currentUsages,
            CountryCode allowedCountry,
            Set<UserId> usedByUsers,
            long version) {
        this.id = id;
        this.code = code;
        this.createdAt = createdAt;
        this.maxUsages = maxUsages;
        this.currentUsages = currentUsages;
        this.allowedCountry = allowedCountry;
        this.usedByUsers = new HashSet<>(usedByUsers);
        this.version = version;
    }

    public static Coupon create(CouponCode code, int maxUsages, CountryCode allowedCountry, Instant now) {
        if (maxUsages <= 0) {
            throw new IllegalArgumentException("maxUsages must be positive, was: " + maxUsages);
        }
        return new Coupon(CouponId.generate(), code, now, maxUsages, 0, allowedCountry, Set.of(), 0L);
    }

    public static Coupon reconstitute(
            CouponId id,
            CouponCode code,
            Instant createdAt,
            int maxUsages,
            int currentUsages,
            CountryCode allowedCountry,
            Set<UserId> usedByUsers,
            long version) {
        return new Coupon(id, code, createdAt, maxUsages, currentUsages, allowedCountry, usedByUsers, version);
    }

    public void use(UserId userId, CountryCode userCountry) {
        if (!allowedCountry.equals(userCountry)) {
            throw new CountryNotAllowedException(code, allowedCountry, userCountry);
        }
        if (currentUsages >= maxUsages) {
            throw new CouponExhaustedException(code);
        }
        if (usedByUsers.contains(userId)) {
            throw new CouponAlreadyUsedException(code, userId);
        }
        currentUsages++;
        usedByUsers.add(userId);
    }

    public boolean isExhausted() {
        return currentUsages >= maxUsages;
    }

    public CouponId getId() {
        return id;
    }

    public CouponCode getCode() {
        return code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getMaxUsages() {
        return maxUsages;
    }

    public int getCurrentUsages() {
        return currentUsages;
    }

    public CountryCode getAllowedCountry() {
        return allowedCountry;
    }

    public Set<UserId> getUsedByUsers() {
        return Collections.unmodifiableSet(usedByUsers);
    }

    public long getVersion() {
        return version;
    }
}
