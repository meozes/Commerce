package kr.hhplus.be.server.domain.coupon.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CouponIssueRequestInfo {
    private final Long userId;
    private final Long couponId;
    private final LocalDateTime requestedAt;

    public static CouponIssueRequestInfo of(Long userId, Long couponId, LocalDateTime requestedAt){
        return CouponIssueRequestInfo.builder()
                .userId(userId)
                .couponId(couponId)
                .requestedAt(requestedAt)
                .build();
    }
}
