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
public class OrderDataPlatformEvent {
    private Long orderId;

    public static OrderDataPlatformEvent from(OrderCompletedEvent event) {
        return OrderDataPlatformEvent.builder()
                .orderId(event.getOrderId())
                .build();
    }
}
