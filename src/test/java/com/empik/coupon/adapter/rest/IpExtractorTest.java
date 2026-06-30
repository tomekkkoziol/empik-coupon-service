package com.empik.coupon.adapter.rest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class IpExtractorTest {

    @Test
    void should_return_remote_addr_when_no_forwarded_header() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.5");

        assertThat(IpExtractor.extract(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void should_prefer_forwarded_header_over_remote_addr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5");

        assertThat(IpExtractor.extract(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void should_take_leftmost_ip_from_forwarded_chain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 70.41.3.18, 150.172.238.178");

        assertThat(IpExtractor.extract(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void should_fall_back_to_remote_addr_when_forwarded_header_blank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "   ");

        assertThat(IpExtractor.extract(request)).isEqualTo("10.0.0.1");
    }
}
