package kr.hhplus.be.server.domain.product.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Builder
public class ProductRankInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Integer rank;
    private final Long productId;
    private final String productName;
    private final Integer totalQuantitySold;
    private final Integer price;

    private LocalDate startDate;
    private LocalDate endDate;

    // 기본 생성자 추가 (직렬화/역직렬화에 필요)
    public ProductRankInfo() {
        this.rank = null;
        this.productId = null;
        this.productName = null;
        this.totalQuantitySold = null;
        this.price = null;
    }

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

    public ProductRankInfo(Long productId, String productName, Integer totalQuantitySold, Integer price) {
        this.rank = null;
        this.productId = productId;
        this.productName = productName;
        this.totalQuantitySold = totalQuantitySold;
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

    public ProductRankInfo withStartDateAndEndDate(LocalDate startDate, LocalDate endDate) {
        return ProductRankInfo.builder()
                .rank(this.rank)
                .productId(this.productId)
                .productName(this.productName)
                .totalQuantitySold(this.totalQuantitySold)
                .price(this.price)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
