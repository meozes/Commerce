package kr.hhplus.be.server.domain.order.dto;

import kr.hhplus.be.server.interfaces.order.request.OrderItemRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemCommand {
    private final Long productId;
    private final String productName;
    private final Integer quantity;
    private final Integer price;

    public static OrderItemCommand from(OrderItemRequest request) {
        return new OrderItemCommand(
                request.getProductId(),
                request.getProductName(),
                request.getQuantity(),
                request.getPrice()
        );
    }
}