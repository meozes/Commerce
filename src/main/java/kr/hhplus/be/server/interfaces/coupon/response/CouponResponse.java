package kr.hhplus.be.server.interfaces.coupon.response;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponResponse {
    private Long couponId;
    private String couponName;
    private Long userId;
    private Integer discountAmount;
    private LocalDate dueDate;
    private CouponStatusType couponStatus;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;

    public static CouponResponse of(Coupon coupon, IssuedCoupon issuedCoupon){
        return CouponResponse.builder()
                .couponId(coupon.getId())
                .couponName(coupon.getCouponName())
                .userId(issuedCoupon.getUserId())
                .discountAmount(coupon.getDiscountAmount())
                .dueDate(coupon.getDueDate())
                .couponStatus(issuedCoupon.getCouponStatus())
                .issuedAt(issuedCoupon.getIssuedAt())
                .usedAt(issuedCoupon.getUsedAt())
                .build();
    }
}
