package kr.hhplus.be.server.domain.product;


import kr.hhplus.be.server.domain.product.dto.ProductInfo;
import kr.hhplus.be.server.domain.product.dto.ProductSearch;
import kr.hhplus.be.server.domain.product.dto.ProductSearchQuery;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.domain.product.usecase.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ProductService productService;


    @Test
    @DisplayName("상품 조회 성공")
    void getProducts_Success() {
        // given

        ProductSearchQuery query = ProductSearchQuery.of(0, 10);
        PageRequest pageRequest = query.toPageRequest();  // PageRequest 생성

        Product product1 = new Product(1L, "촉촉한 쿠키", 5000);
        Product product2 = new Product(2L, "촉촉한 우유", 10000);
        List<Product> products = Arrays.asList(product1, product2);

        Stock stock1 = new Stock(1L, product1, 100, 30);
        Stock stock2 = new Stock(2L, product2, 200, 30);
        List<Stock> stocks = Arrays.asList(stock1, stock2);

        Page<Product> productPage = new PageImpl<>(products, pageRequest, products.size());

        when(productRepository.getProducts(any(PageRequest.class))).thenReturn(productPage);
        when(stockRepository.getStocks(anyList())).thenReturn(stocks);

        // when
        Page<ProductInfo> result = productService.getProducts(query);

        // then
        assertAll(
                () -> assertEquals(2, result.getContent().size()),
                () -> assertEquals(product1.getId(), result.getContent().get(0).getProductId()),
                () -> assertEquals(product1.getProductName(), result.getContent().get(0).getProductName()),
                () -> assertEquals(stock1.getRemainingStock(), result.getContent().get(0).getRemainingStock()),
                () -> assertEquals(product2.getId(), result.getContent().get(1).getProductId()),
                () -> assertEquals(product2.getProductName(), result.getContent().get(1).getProductName()),
                () -> assertEquals(stock2.getRemainingStock(), result.getContent().get(1).getRemainingStock())
        );

        verify(productRepository).getProducts(any(PageRequest.class));
        verify(stockRepository).getStocks(anyList());
    }

    @Test
    @DisplayName("단일 상품 조회 성공")
    void getProduct_Success() {
        // given
        Long productId = 1L;
        ProductSearch productSearch = ProductSearch.of(productId);

        Product product = new Product(productId, "촉촉한 쿠키", 5000);
        Stock stock = new Stock(1L, product, 100, 30);

        when(productRepository.getProduct(productId)).thenReturn(Optional.of(product));
        when(stockRepository.getStock(productId)).thenReturn(stock);

        // when
        ProductInfo result = productService.getProduct(productSearch);

        // then
        assertAll(
                () -> assertEquals(productId, result.getProductId()),
                () -> assertEquals(product.getProductName(), result.getProductName()),
                () -> assertEquals(product.getPrice(), result.getPrice()),
                () -> assertEquals(stock.getRemainingStock(), result.getRemainingStock())
        );

        verify(productRepository).getProduct(productId);
        verify(stockRepository).getStock(productId);
    }

    @Test
    @DisplayName("유효하지 않은 상품 ID - 조회 실패")
    void getProduct_InvalidProductId() {
        ProductSearch productSearch = ProductSearch.of(-1L);

        assertThrows(IllegalArgumentException.class,
                () -> productService.getProduct(productSearch));
    }


}
