package kr.hhplus.be.server.domain.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;

@Getter
@Builder
@AllArgsConstructor
public class CouponSearch {
    private final Long userId;
    private int page;
    private int size;

    public static CouponSearch of(Long userId, int page, int size) {
        return CouponSearch.builder()
                .userId(userId)
                .page(page)
                .size(size)
                .build();
    }

    public PageRequest toPageRequest() {
        return PageRequest.of(page, size);
    }
}
