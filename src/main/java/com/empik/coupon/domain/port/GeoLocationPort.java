package com.empik.coupon.domain.port;

import com.empik.coupon.domain.model.CountryCode;

public interface GeoLocationPort {

    CountryCode resolveCountry(String ipAddress);
}
