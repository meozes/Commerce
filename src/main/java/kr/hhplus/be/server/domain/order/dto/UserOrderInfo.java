package kr.hhplus.be.server.domain.order.dto;

import kr.hhplus.be.server.domain.order.entity.Order;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserOrderInfo {
    private final Long orderId;
    private final Long userId;
    private final Integer finalAmount;

    public static UserOrderInfo of(Order order) {
        return UserOrderInfo.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .finalAmount(order.getFinalAmount())
                .build();
    }

}
