package kr.hhplus.be.server.interfaces.order;


import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.domain.product.usecase.ProductService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.interfaces.order.request.OrderItemRequest;
import kr.hhplus.be.server.interfaces.order.request.OrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.NoSuchElementException;
import java.util.concurrent.*;
import org.springframework.http.MediaType;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class OrderStockConcurrentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private ProductService productService;


    private List<Long> userIds = List.of(50L, 51L);
    private Product testProduct;
    private final int THREAD_COUNT = 2;
    private final int ORDER_QUANTITY_PER_USER = 3;
    private final int INITIAL_STOCK = 10;

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("hhplus")
            .withUsername("application")
            .withPassword("application");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @BeforeAll
    static void beforeAll() {
        mysql.start();
    }

    @AfterAll
    static void afterAll() {
        mysql.stop();
    }

    @BeforeEach
    void setUp() {
        // 1. Product 및 Stock 초기화
        testProduct = productRepository.save(Product.builder()
                .productName("쿠키")
                .price(10000)
                .build());

        stockRepository.save(Stock.builder()
                .product(testProduct)
                .originStock(INITIAL_STOCK)
                .remainingStock(INITIAL_STOCK)
                .build());

        // 2. Balance 초기화
        for (Long userId : userIds) {
            balanceRepository.save(Balance.builder()
                    .userId(userId)
                    .balance(50000)
                    .build());
        }
    }

    @Test
    @DisplayName("주문 요청 API - 동시성 테스트. 비관적 락을 통해 재고 차감 시 정확한 재고를 관리한다.")
    void stockDeduction_Concurrently() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        // when
        for (Long userId : userIds) {
            executorService.submit(() -> {
                try {
                    OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                            .productId(testProduct.getId())
                            .productName(testProduct.getProductName())
                            .quantity(ORDER_QUANTITY_PER_USER)
                            .price(testProduct.getPrice())
                            .build();

                    OrderRequest orderRequest = OrderRequest.builder()
                            .userId(userId)
                            .items(List.of(orderItemRequest))
                            .build();

                    mockMvc.perform(
                                    MockMvcRequestBuilders
                                            .post("/api/orders")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(orderRequest)))
                            .andExpect(status().isOk());

                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 요청이 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        Stock finalStock = stockRepository.findById(testProduct.getId())
                .orElseThrow(() -> new NoSuchElementException("재고를 찾을 수 없습니다"));

        assertThat(finalStock.getRemainingStock())
                .isEqualTo(INITIAL_STOCK - (ORDER_QUANTITY_PER_USER * THREAD_COUNT));

        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(THREAD_COUNT);
    }
}
