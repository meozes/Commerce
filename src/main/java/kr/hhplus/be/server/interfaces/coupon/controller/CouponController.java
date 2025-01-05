package kr.hhplus.be.server.interfaces.coupon.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.interfaces.common.ApiResponse;
import kr.hhplus.be.server.interfaces.coupon.response.CouponResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Tag(name = "쿠폰 API", description = "쿠폰 발급/조회 API")
@RestController
@RequestMapping("api/coupons")
public class CouponController {

    @Operation(summary = "쿠폰 조회", description = "보유한 쿠폰을 조회합니다.")
    @GetMapping("/{userId}")
    public ApiResponse<CouponResponse> getCoupons(
            @Parameter(name = "userId", description = "사용자 ID", required = true)
            @PathVariable("userId") Long userId
    ) {
        Coupon coupon = new Coupon(1L, "10000원 할인 쿠폰", 10000, 100, 10, LocalDate.of(2025, 12, 31));
        IssuedCoupon issuedCoupon = new IssuedCoupon(1L, userId, coupon, null , CouponStatusType.USED, LocalDateTime.of(2024, 12, 31, 0, 0), LocalDateTime.of(2025, 1, 3, 0, 0));
        return ApiResponse.ok(CouponResponse.of(coupon, issuedCoupon));
    }


    @Operation(summary = "쿠폰 발급", description = "쿠폰을 발급합니다.")
    @PostMapping("/{userId}/{couponId}")
    public ApiResponse<CouponResponse> issueCoupon(
            @Parameter(name = "userId", description = "사용자 ID", required = true)
            @PathVariable("userId") Long userId,
            @Parameter(name = "couponId", description = "쿠폰 ID", required = true)
            @PathVariable("couponId") Long couponId
    ) {
        Coupon coupon = new Coupon(couponId, "10000원 할인 쿠폰", 10000, 100, 10, LocalDate.of(2025, 12, 31));
        IssuedCoupon issuedCoupon = new IssuedCoupon(1L, userId, coupon, null , CouponStatusType.NEW, LocalDateTime.now(), null);
        return ApiResponse.ok(CouponResponse.of(coupon, issuedCoupon));
    }
}
