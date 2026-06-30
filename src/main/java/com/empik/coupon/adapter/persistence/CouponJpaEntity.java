package com.empik.coupon.adapter.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "coupons")
class CouponJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;

    @Column(name = "max_usages", nullable = false)
    private int maxUsages;

    @Column(name = "current_usages", nullable = false)
    private int currentUsages;

    @Column(name = "allowed_country", nullable = false, length = 2)
    private String allowedCountry;

    // Eagerly loaded — bounded by maxUsages, so collection size is predictable
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "coupon_usages",
            joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "user_id", nullable = false)
    private Set<String> usedByUserIds = new HashSet<>();

    @Version
    private long version;

    protected CouponJpaEntity() {
    }

    UUID getId() { return id; }
    void setId(UUID id) { this.id = id; }

    String getCode() { return code; }
    void setCode(String code) { this.code = code; }

    Instant getCreatedAt() { return createdAt; }
    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    int getMaxUsages() { return maxUsages; }
    void setMaxUsages(int maxUsages) { this.maxUsages = maxUsages; }

    int getCurrentUsages() { return currentUsages; }
    void setCurrentUsages(int currentUsages) { this.currentUsages = currentUsages; }

    String getAllowedCountry() { return allowedCountry; }
    void setAllowedCountry(String allowedCountry) { this.allowedCountry = allowedCountry; }

    Set<String> getUsedByUserIds() { return usedByUserIds; }
    void setUsedByUserIds(Set<String> usedByUserIds) { this.usedByUserIds = usedByUserIds; }

    long getVersion() { return version; }
    void setVersion(long version) { this.version = version; }
}
