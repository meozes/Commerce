package kr.hhplus.be.server.interfaces.order.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "주문 상품은 필수입니다.")
    @Size(min = 1, message = "최소 1개 이상의 상품을 주문해야 합니다.")
    private List<OrderItemRequest> items;

    private Long couponId;
}
