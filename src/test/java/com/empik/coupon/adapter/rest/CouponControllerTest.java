package com.empik.coupon.adapter.rest;

import com.empik.coupon.application.facade.CouponFacade;
import com.empik.coupon.application.command.CreateCouponCommand;
import com.empik.coupon.application.query.CouponView;
import com.empik.coupon.domain.exception.*;
import com.empik.coupon.domain.model.CouponCode;
import com.empik.coupon.domain.model.CouponId;
import com.empik.coupon.domain.model.CountryCode;
import com.empik.coupon.domain.model.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CouponFacade couponFacade;

    @Test
    void POST_coupons_should_return_201_with_location() throws Exception {
        CouponId id = new CouponId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        given(couponFacade.createCoupon(any(CreateCouponCommand.class))).willReturn(id);

        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"WIOSNA","maxUsages":100,"countryCode":"PL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/coupons/WIOSNA")))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("WIOSNA"));
    }

    @Test
    void POST_coupons_should_return_400_for_missing_fields() throws Exception {
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maxUsages":100}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_coupons_should_return_400_for_zero_max_usages() throws Exception {
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"WIOSNA","maxUsages":0,"countryCode":"PL"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_coupons_should_return_409_when_code_exists() throws Exception {
        given(couponFacade.createCoupon(any())).willThrow(
                new CouponCodeAlreadyExistsException(new CouponCode("WIOSNA")));

        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"WIOSNA","maxUsages":10,"countryCode":"PL"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Coupon Code Already Exists"));
    }

    @Test
    void POST_usages_should_return_200_on_success() throws Exception {
        mockMvc.perform(post("/api/v1/coupons/WIOSNA/usages")
                        .header("X-Forwarded-For", "5.10.10.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1"}
                                """))
                .andExpect(status().isOk());

        then(couponFacade).should().useCoupon(any());
    }

    @Test
    void POST_usages_should_return_404_when_coupon_not_found() throws Exception {
        willThrow(new CouponNotFoundException(new CouponCode("GHOST")))
                .given(couponFacade).useCoupon(any());

        mockMvc.perform(post("/api/v1/coupons/GHOST/usages")
                        .header("X-Forwarded-For", "5.10.10.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Coupon Not Found"));
    }

    @Test
    void POST_usages_should_return_409_when_exhausted() throws Exception {
        willThrow(new CouponExhaustedException(new CouponCode("WIOSNA")))
                .given(couponFacade).useCoupon(any());

        mockMvc.perform(post("/api/v1/coupons/WIOSNA/usages")
                        .header("X-Forwarded-For", "5.10.10.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Coupon Exhausted"));
    }

    @Test
    void POST_usages_should_return_403_when_country_not_allowed() throws Exception {
        willThrow(new CountryNotAllowedException(
                new CouponCode("WIOSNA"), new CountryCode("PL"), new CountryCode("DE")))
                .given(couponFacade).useCoupon(any());

        mockMvc.perform(post("/api/v1/coupons/WIOSNA/usages")
                        .header("X-Forwarded-For", "84.10.10.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Country Not Allowed"));
    }

    @Test
    void POST_usages_should_return_409_when_already_used() throws Exception {
        willThrow(new CouponAlreadyUsedException(new CouponCode("WIOSNA"), new UserId("user-1")))
                .given(couponFacade).useCoupon(any());

        mockMvc.perform(post("/api/v1/coupons/WIOSNA/usages")
                        .header("X-Forwarded-For", "5.10.10.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Coupon Already Used"));
    }

    @Test
    void POST_usages_should_return_400_for_missing_user_id() throws Exception {
        mockMvc.perform(post("/api/v1/coupons/WIOSNA/usages")
                        .header("X-Forwarded-For", "5.10.10.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        then(couponFacade).should(never()).useCoupon(any());
    }

    @Test
    void GET_coupon_should_return_200_with_details() throws Exception {
        CouponView view = new CouponView(
                "11111111-1111-1111-1111-111111111111",
                "WIOSNA",
                Instant.parse("2024-01-01T12:00:00Z"),
                100,
                42,
                "PL",
                false);
        given(couponFacade.getCoupon(any())).willReturn(view);

        mockMvc.perform(get("/api/v1/coupons/WIOSNA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WIOSNA"))
                .andExpect(jsonPath("$.maxUsages").value(100))
                .andExpect(jsonPath("$.currentUsages").value(42))
                .andExpect(jsonPath("$.exhausted").value(false));
    }

    @Test
    void GET_coupon_should_return_404_when_not_found() throws Exception {
        given(couponFacade.getCoupon(any())).willThrow(
                new CouponNotFoundException(new CouponCode("GHOST")));

        mockMvc.perform(get("/api/v1/coupons/GHOST"))
                .andExpect(status().isNotFound());
    }
}
