package com.empik.coupon.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CouponIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("coupon_it")
            .withUsername("coupon")
            .withPassword("coupon");

    static WireMockServer wireMock;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("coupon.geo.api-url", () -> wireMock.baseUrl() + "/json");
        registry.add("coupon.geo.bypass-private-ips", () -> "false");
    }

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Autowired
    TestRestTemplate restTemplate;

    private static final String POLISH_IP = "5.10.10.10";
    private static final String GERMAN_IP = "84.10.10.10";

    private void stubGeoIp(String ip, String countryCode) {
        wireMock.stubFor(get(urlPathEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\",\"countryCode\":\"" + countryCode + "\"}")));
    }

    @Test
    void should_create_coupon_and_use_it_successfully() {
        stubGeoIp(POLISH_IP, "PL");

        // Create coupon
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                "/api/v1/coupons",
                Map.of("code", "INTTEST1", "maxUsages", 5, "countryCode", "PL"),
                Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Use coupon
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", POLISH_IP);
        HttpEntity<Map<String, String>> useRequest =
                new HttpEntity<>(Map.of("userId", "user-1"), headers);

        ResponseEntity<Void> useResponse = restTemplate.exchange(
                "/api/v1/coupons/INTTEST1/usages",
                HttpMethod.POST,
                useRequest,
                Void.class);
        assertThat(useResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify usage count
        ResponseEntity<Map> detailsResponse = restTemplate.getForEntity(
                "/api/v1/coupons/INTTEST1", Map.class);
        assertThat(detailsResponse.getBody()).containsEntry("currentUsages", 1);
    }

    @Test
    void should_reject_usage_from_wrong_country() {
        stubGeoIp(GERMAN_IP, "DE");

        restTemplate.postForEntity(
                "/api/v1/coupons",
                Map.of("code", "INTTEST2", "maxUsages", 5, "countryCode", "PL"),
                Map.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", GERMAN_IP);
        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(Map.of("userId", "user-1"), headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/coupons/INTTEST2/usages", HttpMethod.POST, request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void should_reject_second_usage_by_same_user() {
        stubGeoIp(POLISH_IP, "PL");

        restTemplate.postForEntity(
                "/api/v1/coupons",
                Map.of("code", "INTTEST3", "maxUsages", 10, "countryCode", "PL"),
                Map.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", POLISH_IP);
        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(Map.of("userId", "user-1"), headers);

        restTemplate.exchange("/api/v1/coupons/INTTEST3/usages", HttpMethod.POST, request, Void.class);
        ResponseEntity<Map> secondAttempt = restTemplate.exchange(
                "/api/v1/coupons/INTTEST3/usages", HttpMethod.POST, request, Map.class);

        assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(secondAttempt.getBody()).containsEntry("title", "Coupon Already Used");
    }

    @Test
    void should_reject_usage_when_coupon_exhausted() {
        stubGeoIp(POLISH_IP, "PL");

        restTemplate.postForEntity(
                "/api/v1/coupons",
                Map.of("code", "INTTEST4", "maxUsages", 1, "countryCode", "PL"),
                Map.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", POLISH_IP);

        HttpEntity<Map<String, String>> first = new HttpEntity<>(Map.of("userId", "user-1"), headers);
        restTemplate.exchange("/api/v1/coupons/INTTEST4/usages", HttpMethod.POST, first, Void.class);

        HttpEntity<Map<String, String>> second = new HttpEntity<>(Map.of("userId", "user-2"), headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/coupons/INTTEST4/usages", HttpMethod.POST, second, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("title", "Coupon Exhausted");
    }

    @Test
    void should_reject_duplicate_coupon_code() {
        restTemplate.postForEntity(
                "/api/v1/coupons",
                Map.of("code", "INTTEST5", "maxUsages", 5, "countryCode", "PL"),
                Map.class);

        ResponseEntity<Map> duplicate = restTemplate.postForEntity(
                "/api/v1/coupons",
                Map.of("code", "inttest5", "maxUsages", 10, "countryCode", "DE"),
                Map.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    /**
     * Verifies that under concurrent load exactly maxUsages usages are recorded
     * and the rest are rejected — the "kto pierwszy ten lepszy" requirement.
     */
    @Test
    void should_honour_max_usages_under_concurrent_load() throws Exception {
        int maxUsages = 5;
        int totalRequests = 20;
        String couponCode = "CONCURRENT1";

        stubGeoIp(POLISH_IP, "PL");

        restTemplate.postForEntity(
                "/api/v1/coupons",
                Map.of("code", couponCode, "maxUsages", maxUsages, "countryCode", "PL"),
                Map.class);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            String userId = "concurrent-user-" + i;
            executor.submit(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-Forwarded-For", POLISH_IP);
                    HttpEntity<Map<String, String>> request =
                            new HttpEntity<>(Map.of("userId", userId), headers);

                    ResponseEntity<Void> response = restTemplate.exchange(
                            "/api/v1/coupons/" + couponCode + "/usages",
                            HttpMethod.POST, request, Void.class);

                    if (response.getStatusCode() == HttpStatus.OK) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        ResponseEntity<Map> details = restTemplate.getForEntity(
                "/api/v1/coupons/" + couponCode, Map.class);

        assertThat(successCount.get()).isEqualTo(maxUsages);
        assertThat(details.getBody()).containsEntry("currentUsages", maxUsages);
        assertThat(details.getBody()).containsEntry("exhausted", true);
    }
}
