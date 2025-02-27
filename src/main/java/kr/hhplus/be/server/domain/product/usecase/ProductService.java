package kr.hhplus.be.server.domain.product.usecase;

import kr.hhplus.be.server.domain.order.dto.OrderItemCommand;
import kr.hhplus.be.server.domain.product.dto.ProductInfo;
import kr.hhplus.be.server.domain.product.dto.ProductSearch;
import kr.hhplus.be.server.domain.product.dto.ProductSearchQuery;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    /**
     * 상품 단건 조회하기
     */
    public ProductInfo getProduct(ProductSearch productSearch) {

        if (productSearch.getProductId() < 0){
            throw new IllegalArgumentException(ErrorCode.INVALID_PRODUCT_ID.getMessage());
        }
        Product product = productRepository.getProduct(productSearch.getProductId()).orElseThrow(
                () -> new NoSuchElementException(ErrorCode.PRODUCT_NOT_FOUND.getMessage())
        );
        Stock stock = stockRepository.getStock(productSearch.getProductId()).orElseThrow(
                () -> new NoSuchElementException(ErrorCode.PRODUCT_STOCK_NOT_FOUND.getMessage())
        );

        return ProductInfo.of(product, stock);
    }

    /**
     * 상품 전체 조회하기
     */
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

/**
 * 상품 존재 여부 확인
 */
public void getOrderProduct(List<OrderItemCommand> orderItems) {
    orderItems.forEach(item -> {
        ProductSearch productSearch = ProductSearch.of(item.getProductId());
        getProduct(productSearch);
    });
}

public void validateProducts(List<OrderItemCommand> orderItems) {
    orderItems.forEach(item -> {
        productRepository.getProduct(item.getProductId())
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.PRODUCT_NOT_FOUND.getMessage()));
    });
}
}
