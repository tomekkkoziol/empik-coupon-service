package com.empik.coupon.domain.model;

import java.util.Objects;

public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "UserId cannot be null");
        value = value.strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException("UserId cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
