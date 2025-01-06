package kr.hhplus.be.server.domain.product.dto;

import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import lombok.*;

@Getter
@Builder
public class ProductInfo {
    private final Long productId;
    private final String productName;
    private final Integer price;
    private final Integer remainingStock;

    public static ProductInfo of(Product product, Stock stock) {
        return ProductInfo.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .price(product.getPrice())
                .remainingStock(stock.getRemainingStock())
                .build();
    }

}
