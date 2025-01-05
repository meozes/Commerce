package kr.hhplus.be.server.interfaces.product.response;

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

    public static ProductResponse of(Product product, Stock stock) {
        return ProductResponse.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .price(product.getPrice())
                .remainingStock(stock.getRemainingStock())
                .build();
    }
}
