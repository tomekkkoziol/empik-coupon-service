package com.empik.coupon.adapter.geolocation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
class GeoLocationConfig {

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    RestClient geoLocationRestClient(GeoLocationProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.apiUrl())
                .build();
    }
}
