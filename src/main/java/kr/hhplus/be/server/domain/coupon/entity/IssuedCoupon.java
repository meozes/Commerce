package kr.hhplus.be.server.domain.coupon.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.domain.order.entity.Order;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    private CouponStatusType couponStatus;

    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    public void use() {
        if (CouponStatusType.USED.equals(this.couponStatus)) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다. id=" + this.id);
        }

        if (this.coupon.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("만료된 쿠폰입니다. id=" + this.id);
        }

        this.couponStatus = CouponStatusType.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void assignToOrder(Order order) {
        this.order = order;
    }
}
