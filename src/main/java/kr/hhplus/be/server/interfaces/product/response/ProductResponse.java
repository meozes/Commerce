package kr.hhplus.be.server.interfaces.product.response;

import kr.hhplus.be.server.domain.product.dto.ProductInfo;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductResponse {
    private Long productId;
    private String productName;
    private Integer price;
    private Integer remainingStock;

    public static ProductResponse from(ProductInfo info) {
        return ProductResponse.builder()
                .productId(info.getProductId())
                .productName(info.getProductName())
                .price(info.getPrice())
                .remainingStock(info.getRemainingStock())
                .build();
    }
}
