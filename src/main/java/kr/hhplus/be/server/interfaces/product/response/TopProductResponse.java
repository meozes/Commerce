package kr.hhplus.be.server.interfaces.product.response;

import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TopProductResponse {

    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final List<ProductRankInfo> products;

    public static TopProductResponse of(List<ProductRankInfo> info) {
        return TopProductResponse.builder()
                .startDate(info.getStart)
                .endDate(endDate)
                .products(info)
                .build();
    }

}
