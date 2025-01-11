package kr.hhplus.be.server.domain.product.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;


@Getter
@Builder
public class ProductSearchQuery {
    private final int page;
    private final int size;

    public static ProductSearchQuery of(int page, int size) {
        return ProductSearchQuery.builder()
                .page(page)
                .size(size)
                .build();
    }

    public PageRequest toPageRequest() {
        return PageRequest.of(page, size);
    }
}
