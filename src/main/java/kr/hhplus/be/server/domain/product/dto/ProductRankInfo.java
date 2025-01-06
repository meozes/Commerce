package kr.hhplus.be.server.domain.product.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductRankInfo {
    private final Integer rank;
    private final Long productId;
    private final String productName;
    private final Integer totalQuantitySold;
    private final Integer price;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
