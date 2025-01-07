package kr.hhplus.be.server.interfaces.product.response;

import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class TopProductResponse {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<ProductRankInfo> products;

    public static TopProductResponse of(List<ProductRankInfo> info) {
        if (info == null || info.isEmpty()) {
            return TopProductResponse.builder()
                    .products(Collections.emptyList())
                    .build();
        }

        ProductRankInfo firstProduct = info.get(0);
        return TopProductResponse.builder()
                .startDate(firstProduct.getStartDate())
                .endDate(firstProduct.getEndDate())
                .products(info)
                .build();
    }

}
