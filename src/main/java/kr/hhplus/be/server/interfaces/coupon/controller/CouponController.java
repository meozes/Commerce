package kr.hhplus.be.server.interfaces.coupon.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponInfo;
import kr.hhplus.be.server.domain.coupon.dto.CouponIssueInfo;
import kr.hhplus.be.server.domain.coupon.usecase.CouponControlService;
import kr.hhplus.be.server.domain.coupon.usecase.CouponFindService;
import kr.hhplus.be.server.interfaces.common.response.ApiResponse;
import kr.hhplus.be.server.domain.coupon.dto.CouponSearch;
import kr.hhplus.be.server.interfaces.coupon.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;



@Tag(name = "쿠폰 API", description = "쿠폰 발급/조회 API")
@RestController
@RequestMapping("api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponFindService couponFindService;
    private final CouponControlService couponControlService;

    @Operation(summary = "쿠폰 조회", description = "보유한 쿠폰을 조회합니다.")
    @GetMapping("/{userId}")
    public ApiResponse<Page<CouponResponse>> getCoupons(
            @Parameter(name = "userId", description = "사용자 ID", required = true)
            @PathVariable("userId") Long userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        CouponSearch couponSearch = CouponSearch.of(userId, page, size);
        Page<CouponInfo> infos = couponFindService.getIssuedCoupons(couponSearch);
        Page<CouponResponse> response = infos.map(CouponResponse::from);
        return ApiResponse.ok(response);
    }


    @Operation(summary = "쿠폰 발급", description = "쿠폰을 발급합니다.")
    @PostMapping("/{userId}/{couponId}")
    public ApiResponse<CouponResponse> issueCoupon(
            @Parameter(name = "userId", description = "사용자 ID", required = true)
            @PathVariable("userId") Long userId,
            @Parameter(name = "couponId", description = "쿠폰 ID", required = true)
            @PathVariable("couponId") Long couponId
    ) {
        CouponCommand command = CouponCommand.of(userId, couponId);
        CouponIssueInfo info = couponControlService.requestCoupon(command);
        return ApiResponse.ok(CouponResponse.from(info));
    }
}
