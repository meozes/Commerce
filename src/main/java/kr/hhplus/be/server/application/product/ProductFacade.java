package kr.hhplus.be.server.application.product;


import kr.hhplus.be.server.domain.order.usecase.OrderFindService;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final OrderFindService orderFindService;

    /**
     * 3일간 인기상품 조회하기
     */
    @Cacheable(value = "topProducts", cacheManager = "cacheManager", key = "'top5' + '_' + T(java.time.LocalDate).now().toString()")
    public List<ProductRankInfo> getTopProducts() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today;
        LocalDate startDate = endDate.minusDays(3);
//        LocalDate endDate = today.minusDays(1);
//        LocalDate startDate = endDate.minusDays(4);

        List<ProductRankInfo> products = orderFindService.getTopProductsByOrderDate(startDate, today);

        return IntStream.range(0, products.size())
                .mapToObj(i -> {
                    ProductRankInfo info = products.get(i);
                    return ProductRankInfo.builder()
                            .rank(i + 1)
                            .productId(info.getProductId())
                            .productName(info.getProductName())
                            .totalQuantitySold(info.getTotalQuantitySold())
                            .price(info.getPrice())
                            .startDate(startDate)
                            .endDate(endDate)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 캐시 갱신용 메소드
    @CacheEvict(value = "topProducts", allEntries = true)
    public void refreshTopProducts() {
        // 메소드 내용은 비어있어도 됩니다.
        // @CacheEvict 애노테이션이 캐시를 제거하는 역할을 합니다.
    }

}
