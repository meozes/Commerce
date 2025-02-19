package kr.hhplus.be.server.application.event;

import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSendEvent {
    private String messageId;
    private Long userId;
    private Long orderId;

    public static OrderSendEvent from(OrderCompletedEvent event) {
        return OrderSendEvent.builder()
                .messageId(event.getMessageId())
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .build();
    }
}
