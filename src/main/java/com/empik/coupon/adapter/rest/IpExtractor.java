package com.empik.coupon.adapter.rest;

import jakarta.servlet.http.HttpServletRequest;

final class IpExtractor {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private IpExtractor() {
    }

    static String extract(HttpServletRequest request) {
        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
