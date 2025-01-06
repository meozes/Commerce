package kr.hhplus.be.server.interfaces.product.response;

import kr.hhplus.be.server.domain.product.dto.ProductInfo;
import lombok.*;

@Getter
@Builder
public class ProductResponse {
    private final Long productId;
    private final String productName;
    private final Integer price;
    private final Integer remainingStock;

    public static ProductResponse from(ProductInfo info) {
        return ProductResponse.builder()
                .productId(info.getProductId())
                .productName(info.getProductName())
                .price(info.getPrice())
                .remainingStock(info.getRemainingStock())
                .build();
    }
}
