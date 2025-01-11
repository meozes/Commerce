package kr.hhplus.be.server.domain.coupon.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponCommand {
    private final Long userId;
    private final Long couponId;

    public static CouponCommand of(Long userId, Long couponId) {
        return CouponCommand.builder()
                .userId(userId)
                .couponId(couponId)
                .build();
    }
}
