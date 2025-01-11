package kr.hhplus.be.server.domain.coupon.dto;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssuedCouponInfo {
    private final IssuedCoupon issuedCoupon;

    public static IssuedCouponInfo from(IssuedCoupon issuedCoupon) {
        return IssuedCouponInfo.builder()
                .issuedCoupon(issuedCoupon)
                .build();
    }
}
