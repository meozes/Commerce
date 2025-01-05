package kr.hhplus.be.server.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Getter
@AllArgsConstructor
public class ProductSearchQuery {
    private final int page;
    private final int size;
    private final String sort;

    public PageRequest toPageRequest() {
        return PageRequest.of(page, size, Sort.by(sort));
    }
}
