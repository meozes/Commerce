package kr.hhplus.be.server.interfaces.order.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer price;
}
