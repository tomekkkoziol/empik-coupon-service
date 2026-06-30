package com.empik.coupon.adapter.geolocation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record IpApiResponse(String status, String countryCode) {

    boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
