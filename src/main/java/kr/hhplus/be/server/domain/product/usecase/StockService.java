package kr.hhplus.be.server.domain.product.usecase;

import kr.hhplus.be.server.domain.order.dto.OrderItemCommand;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;

    /**
     * 재고 차감하기
     */
    @Transactional
    public void deductStock(List<OrderItemCommand> orderItems) {
        orderItems.forEach(item -> {
                    Stock stock = stockRepository.getStockWithLock(item.getProductId())
                            .orElseThrow(() -> new NoSuchElementException(ErrorCode.PRODUCT_STOCK_NOT_FOUND.getMessage()));

                    if (stock.getRemainingStock() < item.getQuantity()){
                        throw new IllegalStateException(
                                String.format(ErrorCode.INSUFFICIENT_STOCK.getMessage() + " 상품ID: %d, 요청수량: %d, 재고수량: %d",
                                        item.getProductId(),
                                        item.getQuantity(),
                                        stock.getRemainingStock())
                        );
                    }
                    stock.deductStock(item.getQuantity());
                    stockRepository.save(stock);
                });
    }

    /**
     * 재고 복구하기
     */
    @Transactional
    public void restoreStock(List<OrderItem> orderItems) {
        orderItems.forEach(item -> {
                    Stock stock = stockRepository.getStockWithLock(item.getProductId())
                                    .orElseThrow(() -> new NoSuchElementException(ErrorCode.PRODUCT_STOCK_NOT_FOUND.getMessage()));
                    stock.restoreStock(item.getQuantity());
                    stockRepository.save(stock);
                });
    }
}
