package kr.hhplus.be.server.domain.coupon.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import lombok.*;

import java.time.LocalDate;


@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String couponName;

    private Integer discountAmount;

    private Integer originalQuantity;

    private Integer remainingQuantity;

    private LocalDate dueDate;

    public void decreaseRemainingQuantity() {
        if (this.remainingQuantity <= 0) {
            throw new IllegalArgumentException("쿠폰이 모두 소진되었습니다. id=" + this.id);
        }
        this.remainingQuantity--;
    }
}
