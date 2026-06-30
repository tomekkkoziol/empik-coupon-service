package com.empik.coupon.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record UseCouponRequest(

        @NotBlank(message = "User ID is required")
        String userId) {
}
