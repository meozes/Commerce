package kr.hhplus.be.server.domain.product.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class ProductSearch {
    private final Long productId;

    public static ProductSearch of (Long productId) {
        return new ProductSearch(
                productId
        );
    }
}
