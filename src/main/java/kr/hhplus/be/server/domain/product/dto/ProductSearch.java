package kr.hhplus.be.server.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class ProductSearch {
    private final Long productId;

    public static ProductSearch of (Long productId) {
        return new ProductSearch(
                productId
        );
    }
}
