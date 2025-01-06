package kr.hhplus.be.server.domain.coupon.dto;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CouponInfo {
    private final Long userId;
    private final Coupon coupon;
    private final IssuedCoupon issuedCoupon;

    public static CouponInfo of(Long userId, Coupon coupon, IssuedCoupon issuedCoupon) {
        return CouponInfo.builder()
                .userId(userId)
                .coupon(coupon)
                .issuedCoupon(issuedCoupon)
                .build();
    }
}
