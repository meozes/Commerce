package kr.hhplus.be.server.application.scheduler;

import kr.hhplus.be.server.application.product.ProductFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductCacheScheduler {

    private final ProductFacade productFacade;

    // 3일마다 실행 (매 3일마다 자정에 실행)
    @Scheduled(cron = "0 0 0 */3 * ?")
    public void evictTopProductsCache() {
        productFacade.refreshTopProducts();
    }
}
