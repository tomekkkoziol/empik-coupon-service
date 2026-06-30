package com.empik.coupon.adapter.geolocation;

import com.empik.coupon.domain.exception.GeoLocationUnavailableException;
import com.empik.coupon.domain.model.CountryCode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class IpApiGeoLocationAdapterTest {

    WireMockServer wireMock;
    IpApiGeoLocationAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        GeoLocationProperties props = new GeoLocationProperties(
                wireMock.baseUrl() + "/json",
                false,
                "PL",
                Duration.ofSeconds(5));

        RestClient restClient = RestClient.builder()
                .baseUrl(props.apiUrl())
                .build();

        adapter = new IpApiGeoLocationAdapter(restClient, props);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void should_return_country_code_for_valid_ip() {
        wireMock.stubFor(get(urlPathEqualTo("/json/5.10.10.10"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status":"success","countryCode":"PL"}
                                """)));

        CountryCode result = adapter.resolveCountry("5.10.10.10");

        assertThat(result).isEqualTo(new CountryCode("PL"));
    }

    @Test
    void should_throw_when_api_returns_fail_status() {
        wireMock.stubFor(get(urlPathEqualTo("/json/0.0.0.0"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status":"fail","message":"reserved range","countryCode":""}
                                """)));

        assertThatThrownBy(() -> adapter.resolveCountry("0.0.0.0"))
                .isInstanceOf(GeoLocationUnavailableException.class)
                .hasMessageContaining("0.0.0.0");
    }

    @Test
    void should_throw_when_api_is_unavailable() {
        wireMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> adapter.resolveCountry("5.10.10.10"))
                .isInstanceOf(GeoLocationUnavailableException.class);
    }

    @Test
    void should_use_default_country_for_private_ip_when_bypass_enabled() {
        GeoLocationProperties props = new GeoLocationProperties(
                wireMock.baseUrl() + "/json",
                true,
                "PL",
                Duration.ofSeconds(5));

        RestClient restClient = RestClient.builder()
                .baseUrl(props.apiUrl())
                .build();

        IpApiGeoLocationAdapter bypassAdapter = new IpApiGeoLocationAdapter(restClient, props);

        CountryCode result = bypassAdapter.resolveCountry("127.0.0.1");

        assertThat(result).isEqualTo(new CountryCode("PL"));
        wireMock.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void should_throw_for_private_ip_when_bypass_disabled() {
        assertThatThrownBy(() -> adapter.resolveCountry("192.168.1.1"))
                .isInstanceOf(GeoLocationUnavailableException.class)
                .hasMessageContaining("192.168.1.1");
    }
}
