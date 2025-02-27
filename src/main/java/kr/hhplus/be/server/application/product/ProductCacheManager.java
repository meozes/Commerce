package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductCacheManager implements ApplicationListener<ApplicationReadyEvent> {

    private final ProductFacade productFacade;
    private final Logger log = LoggerFactory.getLogger(ProductCacheManager.class);

    // 애플리케이션 시작 시 웜업
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        warmUpCache();
    }

    // 스케줄된 캐시 eviction과 즉시 웜업
    @Scheduled(cron = "0 0 0 */3 * ?")
    public void refreshCache() {
        log.info("Scheduled cache refresh starting...");
        productFacade.refreshTopProducts(); // 기존 캐시 제거
        warmUpCache(); // 즉시 새로운 데이터로 웜업
    }

    private void warmUpCache() {
        try {
            log.info("Warming up top products cache...");
            List<ProductRankInfo> products = productFacade.getTopProducts();
            log.info("Cache warmed up successfully. Loaded {} top products.", products.size());
        } catch (Exception e) {
            log.error("Failed to warm up cache: {}", e.getMessage(), e);
        }
    }
}
