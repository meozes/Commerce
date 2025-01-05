package kr.hhplus.be.server.interfaces.product.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Top5ProductResponse {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<ProductRankInfo> products;


    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ProductRankInfo {
        private Integer rank;
        private Long productId;
        private String productName;
        private Integer totalQuantitySold;
        private Integer price;
    }

    public static Top5ProductResponse of(List<ProductRankInfo> products,
                                         LocalDateTime startDate,
                                         LocalDateTime endDate) {
        return Top5ProductResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .products(products)
                .build();
    }
}
