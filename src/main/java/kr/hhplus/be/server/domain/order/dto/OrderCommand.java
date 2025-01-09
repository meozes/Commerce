package kr.hhplus.be.server.domain.order.dto;

import kr.hhplus.be.server.interfaces.order.request.OrderRequest;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class OrderCommand {
    private final Long userId;
    private final List<OrderItemCommand> orderItems;
    private final Long couponId;

    public static OrderCommand from(OrderRequest request) {
        return OrderCommand.builder()
                .userId(request.getUserId())
                .orderItems(request.getItems().stream()
                        .map(OrderItemCommand::from)
                        .collect(Collectors.toList()))
                .couponId(request.getCouponId())
                .build();
    }
}
