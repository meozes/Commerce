package kr.hhplus.be.server.domain.product.usecase;

import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRestoreService {
    private final StockRepository stockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeRestore(List<OrderItem> orderItems) {
        log.info("[재고 복구 시작] orderItems={}", orderItems);

        orderItems.forEach(item -> {
            Stock stock = stockRepository.getStock(item.getProductId())
                    .orElseThrow(() -> new NoSuchElementException(ErrorCode.PRODUCT_STOCK_NOT_FOUND.getMessage()));
            stock.restoreStock(item.getQuantity());
            stockRepository.save(stock);

            log.info("[재고 복구 완료] orderId={}, productId={}, restoredQuantity={}, remainingStock={}",
                    item.getOrder().getId(),
                    item.getProductId(),
                    item.getQuantity(),
                    stock.getRemainingStock());
        });
    }
}
