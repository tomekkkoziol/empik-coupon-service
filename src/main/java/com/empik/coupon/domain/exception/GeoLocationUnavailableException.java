package com.empik.coupon.domain.exception;

public class GeoLocationUnavailableException extends RuntimeException {

    public GeoLocationUnavailableException(String ip, Throwable cause) {
        super("Could not determine country for IP: " + ip, cause);
    }

    public GeoLocationUnavailableException(String ip) {
        super("Could not determine country for IP: " + ip);
    }
}
