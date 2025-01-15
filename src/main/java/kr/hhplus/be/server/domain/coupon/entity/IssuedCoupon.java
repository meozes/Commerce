package kr.hhplus.be.server.domain.coupon.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.interfaces.common.ErrorCode;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private CouponStatusType couponStatus;

    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    public void use() {
        if (CouponStatusType.USED.equals(this.couponStatus)) {
            throw new IllegalStateException(ErrorCode.COUPON_ALREADY_USED.getMessage() + " 사용 일자 = " + this.usedAt);
        }

        if (this.coupon.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException(ErrorCode.COUPON_EXPIRED.getMessage() + " 만료 일자 = " + this.coupon.getDueDate());
        }

        this.couponStatus = CouponStatusType.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void assignOrderToCoupon(Order order) {
        this.orderId = order.getId();
    }
}
