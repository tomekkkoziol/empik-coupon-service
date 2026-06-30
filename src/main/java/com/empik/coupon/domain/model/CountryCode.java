package com.empik.coupon.domain.model;

import java.util.Locale;
import java.util.Objects;

public record CountryCode(String value) {

    public CountryCode {
        Objects.requireNonNull(value, "Country code cannot be null");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (value.length() != 2) {
            throw new IllegalArgumentException(
                    "Country code must be exactly 2 characters (ISO 3166-1 alpha-2), got: '" + value + "'");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
