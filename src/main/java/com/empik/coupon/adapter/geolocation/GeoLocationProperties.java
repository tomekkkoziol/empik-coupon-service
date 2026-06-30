package com.empik.coupon.adapter.geolocation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "coupon.geo")
public record GeoLocationProperties(
        String apiUrl,
        boolean bypassPrivateIps,
        String defaultCountryForPrivateIps,
        Duration timeout) {

    public GeoLocationProperties {
        if (apiUrl == null) apiUrl = "http://ip-api.com/json";
        if (defaultCountryForPrivateIps == null) defaultCountryForPrivateIps = "PL";
        if (timeout == null) timeout = Duration.ofSeconds(5);
    }
}
