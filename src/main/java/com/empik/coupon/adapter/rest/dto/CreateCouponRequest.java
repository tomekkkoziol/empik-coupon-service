package com.empik.coupon.adapter.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(

        @NotBlank(message = "Coupon code is required")
        String code,

        @Min(value = 1, message = "Max usages must be at least 1")
        int maxUsages,

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters (ISO 3166-1 alpha-2)")
        @Pattern(regexp = "[A-Za-z]{2}", message = "Country code must contain only letters")
        String countryCode) {
}
