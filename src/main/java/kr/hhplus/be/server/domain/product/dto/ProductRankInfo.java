package kr.hhplus.be.server.domain.product.dto;

import kr.hhplus.be.server.domain.product.entity.Product;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
public class ProductRankInfo {
    private final Integer rank;
    private final Long productId;
    private final String productName;
    private final Integer totalQuantitySold;
    private final Integer price;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder
    public ProductRankInfo(Integer rank, Long productId, String productName, Integer totalQuantitySold,
                           Integer price, LocalDate startDate, LocalDate endDate) {
        this.rank = rank;
        this.productId = productId;
        this.productName = productName;
        this.totalQuantitySold = totalQuantitySold;
        this.price = price;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public ProductRankInfo(Long productId, String productName, Long totalQuantitySold, Integer price) {
        this.rank = null;
        this.productId = productId;
        this.productName = productName;
        this.totalQuantitySold = totalQuantitySold.intValue();
        this.price = price;
    }

    // rank를 추가하기 위한 메서드
    public ProductRankInfo withRank(int rank) {
        return ProductRankInfo.builder()
                .rank(rank)
                .productId(this.productId)
                .productName(this.productName)
                .totalQuantitySold(this.totalQuantitySold)
                .price(this.price)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .build();
    }
}
