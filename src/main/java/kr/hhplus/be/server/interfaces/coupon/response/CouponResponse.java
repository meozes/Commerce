package kr.hhplus.be.server.interfaces.coupon.response;

import kr.hhplus.be.server.domain.coupon.dto.CouponInfo;
import kr.hhplus.be.server.domain.coupon.dto.CouponIssueInfo;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import lombok.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;



@Getter
@Builder
public class CouponResponse {
    private final Long couponId;
    private final String couponName;
    private final Long userId;
    private final Integer discountAmount;
    private final LocalDate dueDate;
    private final CouponStatusType couponStatus;
    private final LocalDateTime issuedAt;
    private final LocalDateTime usedAt;
    private final LocalDateTime requestedAt;

    public static CouponResponse from(CouponInfo info){
        return CouponResponse.builder()
                .couponId(info.getCoupon().getId())
                .couponName(info.getCoupon().getCouponName())
                .userId(info.getIssuedCoupon().getUserId())
                .discountAmount(info.getCoupon().getDiscountAmount())
                .dueDate(info.getCoupon().getDueDate())
                .couponStatus(info.getIssuedCoupon().getCouponStatus())
                .issuedAt(info.getIssuedCoupon().getIssuedAt())
                .usedAt(info.getIssuedCoupon().getUsedAt())
                .build();
    }

    public static CouponResponse from(CouponIssueInfo info){
        return CouponResponse.builder()
                .couponId(info.getCouponId())
                .userId(info.getUserId())
                .requestedAt(info.getRequestedAt())
                .build();

    }

    public static Page<CouponResponse> fromList(Page<CouponInfo> infoList) {
        return infoList.map(CouponResponse::from);
    }
}
