package kr.hhplus.be.server.domain.product;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.exception.InsufficientStockException;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.domain.product.usecase.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("재고 차감 성공")
    void validateAndDeductStock_Success() {
        // given
        Long productId = 1L;
        int quantity = 2;
        Product product = new Product(productId, "촉촉한 쿠키", 5000);
        Stock stock = new Stock(1L, product, 10, 30);

        when(stockRepository.getStockWithLock(productId)).thenReturn(stock);

        // when
        stockService.deductStock(productId, quantity);

        // then
        assertEquals(28, stock.getRemainingStock());
        verify(stockRepository).getStockWithLock(productId);
        verify(stockRepository).save(stock);
    }

    @Test
    @DisplayName("재고 정보가 존재하지 않을 때 예외 발생")
    void validateAndDeductStock_StockNotFound() {
        // given
        Long productId = 1L;
        Product product = new Product(productId, "촉촉한 쿠키", 5000);

        when(stockRepository.getStockWithLock(productId)).thenReturn(null);

        // when & then
        assertThrows(EntityNotFoundException.class,
                () -> stockService.deductStock(productId, 1));
        verify(stockRepository).getStockWithLock(productId);
    }

    @Test
    @DisplayName("재고 부족 - 예외 발생")
    void validateAndDeductStock_InsufficientStock() {
        // given
        Long productId = 1L;
        Product product = new Product(productId, "촉촉한 쿠키", 5000);
        Stock stock = new Stock(1L, product, 30, 5);
        int requestQuantity = 10;

        when(stockRepository.getStockWithLock(productId)).thenReturn(stock);

        // when & then
        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> stockService.deductStock(productId, requestQuantity));

        assertTrue(exception.getMessage().contains("상품의 재고가 부족합니다"));
        verify(stockRepository).getStockWithLock(productId);
        verify(stockRepository, never()).save(any(Stock.class));
    }
}
