package kr.hhplus.be.server.domain.order.event;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderCompletedEvent {
    private final Long orderId;
    private final Long userId;
    private final OrderStatusType orderStatus;
    private final LocalDateTime completedAt;

    public static OrderCompletedEvent from(Order order) {
        return OrderCompletedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .orderStatus(order.getOrderStatus())
                .completedAt(LocalDateTime.now())
                .build();
    }
}
