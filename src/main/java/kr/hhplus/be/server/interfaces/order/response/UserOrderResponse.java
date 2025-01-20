package kr.hhplus.be.server.interfaces.order.response;

import kr.hhplus.be.server.domain.order.dto.UserOrderInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserOrderResponse {
    private Long orderId;
    private Long userId;
    private Integer finalAmount;

    public static UserOrderResponse from(UserOrderInfo info) {
        return UserOrderResponse.builder()
                .orderId(info.getOrderId())
                .userId(info.getUserId())
                .finalAmount(info.getFinalAmount())
                .build();
    }
}
