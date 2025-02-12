package kr.hhplus.be.server.domain.order.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import lombok.*;

@Entity
@Table(
        name = "orders",
        indexes = @Index(
                name = "idx_orders_created_status",
                columnList = "created_at,order_status"
        )
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Integer originalAmount;

    private Integer finalAmount;

    private Integer discountAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatusType orderStatus;

    public void complete() {
        this.orderStatus = OrderStatusType.COMPLETED;
    }

    public void canceled() {
        this.orderStatus = OrderStatusType.CANCELED;
    }

}
