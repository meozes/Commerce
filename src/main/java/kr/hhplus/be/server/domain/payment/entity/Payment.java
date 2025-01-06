package kr.hhplus.be.server.domain.payment.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long userId;

    private Integer amount;

    private PaymentStatusType paymentStatus;
}
