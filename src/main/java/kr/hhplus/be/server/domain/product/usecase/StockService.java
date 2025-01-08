package kr.hhplus.be.server.domain.product.usecase;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.exception.InsufficientStockException;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    @Transactional
    public void validateAndDeductStock(Long productId, int quantity) {

        Product product = productRepository.getProduct(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. productId: " + productId));
        Stock stock = stockRepository.getStockWithLock(productId);
        if (stock == null) {
            throw new EntityNotFoundException("재고 정보를 찾을 수 없습니다. productId: " + productId);
        }

        if (stock.getRemainingStock() < quantity) {
            throw new InsufficientStockException(
                    String.format("상품의 재고가 부족합니다. 상품ID: %d, 요청수량: %d, 재고수량: %d",
                            productId,
                            quantity,
                            stock.getRemainingStock()
                    )
            );
        }

        stock.deductStock(quantity);
        stockRepository.save(stock);
    }

}