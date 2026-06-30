package com.empik.coupon.adapter.rest;

import com.empik.coupon.adapter.rest.dto.CouponDetailsResponse;
import com.empik.coupon.adapter.rest.dto.CreateCouponRequest;
import com.empik.coupon.adapter.rest.dto.CreateCouponResponse;
import com.empik.coupon.adapter.rest.dto.UseCouponRequest;
import com.empik.coupon.application.facade.CouponFacade;
import com.empik.coupon.application.command.CreateCouponCommand;
import com.empik.coupon.application.command.UseCouponCommand;
import com.empik.coupon.application.query.GetCouponQuery;
import com.empik.coupon.domain.model.CouponId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/coupons")
class CouponController {

    private final CouponFacade couponFacade;

    CouponController(CouponFacade couponFacade) {
        this.couponFacade = couponFacade;
    }

    @PostMapping
    ResponseEntity<CreateCouponResponse> createCoupon(
            @Valid @RequestBody CreateCouponRequest request,
            UriComponentsBuilder uriBuilder) {
        CouponId id = couponFacade.createCoupon(new CreateCouponCommand(
                request.code(),
                request.maxUsages(),
                request.countryCode()));

        URI location = uriBuilder.path("/api/v1/coupons/{code}")
                .buildAndExpand(request.code().toUpperCase())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(new CreateCouponResponse(id.toString(), request.code().toUpperCase()));
    }

    @PostMapping("/{code}/usages")
    ResponseEntity<Void> useCoupon(
            @PathVariable String code,
            @Valid @RequestBody UseCouponRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = IpExtractor.extract(httpRequest);
        couponFacade.useCoupon(new UseCouponCommand(code, request.userId(), clientIp));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{code}")
    ResponseEntity<CouponDetailsResponse> getCoupon(@PathVariable String code) {
        CouponDetailsResponse response = CouponDetailsResponse.from(
                couponFacade.getCoupon(new GetCouponQuery(code)));
        return ResponseEntity.ok(response);
    }
}
