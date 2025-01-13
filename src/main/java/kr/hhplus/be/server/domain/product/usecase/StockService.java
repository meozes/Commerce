package kr.hhplus.be.server.domain.product.usecase;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.order.dto.OrderItemCommand;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.exception.InsufficientStockException;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        for (OrderItemCommand item : orderItems) {
            Stock stock = stockRepository.getStockWithLock(item.getProductId());
            if (stock == null) {
                throw new EntityNotFoundException("상품의 재고 정보가 없습니다.");
            }
            if (stock.getRemainingStock() < item.getQuantity()){
                    throw new InsufficientStockException(
                            String.format("상품의 재고가 부족합니다. 상품ID: %d, 요청수량: %d, 재고수량: %d",
                                    item.getProductId(),
                                    item.getQuantity(),
                                    stock.getRemainingStock()
                            )
                    );
            }
            stock.deductStock(item.getQuantity());
            stockRepository.save(stock);
        }
    }
}
