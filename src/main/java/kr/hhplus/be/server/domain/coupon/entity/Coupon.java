package kr.hhplus.be.server.domain.coupon.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import kr.hhplus.be.server.interfaces.common.ErrorCode;
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
            throw new IllegalStateException(ErrorCode.COUPON_OUT_OF_STOCK.getMessage());
        }
        this.remainingQuantity--;
    }
}
