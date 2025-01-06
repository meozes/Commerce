package kr.hhplus.be.server.domain.order.dto;

import kr.hhplus.be.server.interfaces.order.request.OrderItemRequest;
import lombok.Getter;

@Getter
public class OrderItemCommand {
    private final Long productId;
    private final Integer quantity;
    private final Integer price;

    public OrderItemCommand(Long productId, Integer quantity, Integer price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public static OrderItemCommand from(OrderItemRequest request) {
        return new OrderItemCommand(
                request.getProductId(),
                request.getQuantity(),
                request.getPrice()
        );
    }
}