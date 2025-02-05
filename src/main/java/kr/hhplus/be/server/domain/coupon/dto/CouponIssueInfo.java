package kr.hhplus.be.server.domain.coupon.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CouponIssueInfo {
    private final Long userId;
    private final Long couponId;
    private final LocalDateTime requestedAt;

    public static CouponIssueInfo of(Long userId, Long couponId, LocalDateTime requestedAt){
        return CouponIssueInfo.builder()
                .userId(userId)
                .couponId(couponId)
                .requestedAt(requestedAt)
                .build();
    }
}
