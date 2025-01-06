package kr.hhplus.be.server.domain.order.dto;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderInfo {
    private final Order order;
    private final List<OrderItem> orderItems;

    public static OrderInfo of(Order order, List<OrderItem> orderItems) {
        return OrderInfo.builder()
                .order(order)
                .orderItems(orderItems)
                .build();
    }
}
