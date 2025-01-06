package kr.hhplus.be.server.interfaces.order.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemRequest {
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @NotNull(message = "상품명은 필수입니다.")
    private String productName;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private Integer quantity;

    @NotNull(message = "상품 가격은 필수입니다.")
    private Integer price;
}