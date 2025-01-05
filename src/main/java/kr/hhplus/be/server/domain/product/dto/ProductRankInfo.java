package kr.hhplus.be.server.domain.product.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRankInfo {
    private Integer rank;
    private Long productId;
    private String productName;
    private Integer totalQuantitySold;
    private Integer price;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
