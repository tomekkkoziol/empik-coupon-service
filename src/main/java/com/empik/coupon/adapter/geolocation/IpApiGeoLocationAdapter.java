package com.empik.coupon.adapter.geolocation;

import com.empik.coupon.domain.exception.GeoLocationUnavailableException;
import com.empik.coupon.domain.model.CountryCode;
import com.empik.coupon.domain.port.GeoLocationPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Set;

@Component
class IpApiGeoLocationAdapter implements GeoLocationPort {

    private static final Set<String> PRIVATE_IP_PREFIXES = Set.of(
            "127.", "10.", "192.168.", "::1", "0:0:0:0:0:0:0:1");

    private final RestClient restClient;
    private final GeoLocationProperties properties;

    IpApiGeoLocationAdapter(RestClient geoLocationRestClient, GeoLocationProperties properties) {
        this.restClient = geoLocationRestClient;
        this.properties = properties;
    }

    @Override
    public CountryCode resolveCountry(String ipAddress) {
        if (isPrivateIp(ipAddress)) {
            if (properties.bypassPrivateIps()) {
                return new CountryCode(properties.defaultCountryForPrivateIps());
            }
            throw new GeoLocationUnavailableException(ipAddress);
        }

        try {
            IpApiResponse response = restClient.get()
                    .uri("/{ip}?fields=status,countryCode", ipAddress)
                    .retrieve()
                    .body(IpApiResponse.class);

            if (response == null || !response.isSuccess()) {
                throw new GeoLocationUnavailableException(ipAddress);
            }

            return new CountryCode(response.countryCode());
        } catch (RestClientException ex) {
            throw new GeoLocationUnavailableException(ipAddress, ex);
        }
    }

    private boolean isPrivateIp(String ip) {
        return PRIVATE_IP_PREFIXES.stream().anyMatch(ip::startsWith);
    }
}
