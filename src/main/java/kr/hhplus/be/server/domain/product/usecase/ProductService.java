package kr.hhplus.be.server.domain.product.usecase;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.product.dto.ProductInfo;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import kr.hhplus.be.server.domain.product.dto.ProductSearch;
import kr.hhplus.be.server.domain.product.dto.ProductSearchQuery;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.exception.InsufficientStockException;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    public ProductInfo getProduct(ProductSearch productSearch) {

        if (productSearch.getProductId() < 0){
            throw new IllegalArgumentException("유효하지 않은 상품 ID 입니다.");
        }
        Product product = productRepository.getProduct(productSearch.getProductId()).orElseThrow();
        Stock stock = stockRepository.getStock(productSearch.getProductId());

        return ProductInfo.of(product, stock);
    }

    public Page<ProductInfo> getProducts(ProductSearchQuery query) {
        Page<Product> products = productRepository.getProducts(query.toPageRequest());

        List<Stock> stocks = stockRepository.getStocks(
                products.getContent().stream()
                        .map(Product::getId)
                        .collect(Collectors.toList())
        );

        Map<Long, Stock> stockMap = stocks.stream()
                .collect(Collectors.toMap(
                        stock -> stock.getProduct().getId(),
                        stock -> stock
                ));

        return products.map(product ->
                ProductInfo.of(product, stockMap.get(product.getId())));
    }

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

//    public List<ProductRankInfo> getTopProducts() {
//
//    }

//    public List<ProductRankInfo> getTopProducts() {
//        LocalDateTime today = LocalDateTime.now();
//        LocalDateTime endDate = today.minusDays(1);
//        LocalDateTime startDate = endDate.minusDays(3);
//        // 주문 테이블에서 가져와야함.
//    }


}
