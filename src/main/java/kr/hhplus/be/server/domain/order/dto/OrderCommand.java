package kr.hhplus.be.server.domain.order.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
public class OrderCommand {
    private final Long userId;
    private final List<OrderItemCommand> orderItems;
    private final Long couponId;

    @Getter
    public static class OrderItemCommand {
        private final Long productId;
        private final Integer quantity;
        private final Integer price;


        public OrderItemCommand(Long productId, Integer quantity, Integer price) {
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
        }
    }
}
