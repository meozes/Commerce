package kr.hhplus.be.server.interfaces.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import kr.hhplus.be.server.interfaces.payment.request.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class StockRestoringRedisLockIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(StockDeductionRedisLockIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductRepository productRepository;


    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("hhplus")
            .withUsername("application")
            .withPassword("application");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

//    @BeforeEach
//    void setUp() {
//
//    }

    @Test
    @DisplayName("Redisson 분산 락을 통한 동시성 재고 복구 테스트 - 정상 케이스")
    void stockRestoring_withRLock() throws Exception {

        // given
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 1. 상품 생성
        Product product = Product.builder()
                .productName("우유")
                .price(10000)
                .build();
        productRepository.save(product);

        // 2. 재고 초기 데이터 설정
        Stock stock = Stock.builder()
                .product(product)
                .originStock(100)
                .remainingStock(90) // 이미 10개가 차감된 상태로 가정
                .build();
        stockRepository.save(stock);

        // 3. 주문 생성 및 주문 상품 생성을 위한 리스트
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1; // 각기 다른 유저 ID 설정
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 각 쓰레드마다 새로운 주문 생성 (서로 다른 유저)
                    Order order = Order.builder()
                            .userId(userId)  // 각기 다른 유저 ID 사용
                            .orderStatus(OrderStatusType.PENDING)
                            .originalAmount(10000)
                            .finalAmount(10000)
                            .discountAmount(0)
                            .build();
                    orderRepository.save(order);

                    // 주문 상품 생성
                    OrderItem orderItem = OrderItem.builder()
                            .order(order)
                            .productId(product.getId())
                            .productName(product.getProductName())
                            .quantity(10)
                            .productPrice(product.getPrice())
                            .totalPrice(product.getPrice() * 10)
                            .build();
                    orderItemRepository.save(orderItem);

                    // 결제 요청 데이터 생성
                    PaymentRequest request = new PaymentRequest(
                            userId,  // 각기 다른 유저 ID 사용
                            order.getId(),
                            order.getFinalAmount()
                    );

                    // 결제 요청 실행
                    mockMvc.perform(
                                    MockMvcRequestBuilders
                                            .post("/api/payments")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isBadRequest())
                            .andExpect(jsonPath("$.message").value(ErrorCode.INSUFFICIENT_BALANCE.getMessage()));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            }, executorService);

            futures.add(future);
        }

        // 모든 쓰레드의 작업이 완료될 때까지 대기
        latch.await();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // then
        // 1. 재고가 정상적으로 복구되었는지 확인
        Stock updatedStock = stockRepository.getStock(stock.getId()).orElseThrow();
        assertEquals(100, updatedStock.getRemainingStock().intValue()); // 초기 재고량으로 복구

        // 2. 모든 주문의 상태가 취소로 변경되었는지 확인
        List<Order> orders = orderRepository.findAll();
        assertTrue(orders.stream()
                .allMatch(order -> order.getOrderStatus() == OrderStatusType.CANCELED));

        // 3. 각 유저별 주문 상태 확인 및 로그
        orders.forEach(order -> {
            log.info("User {} order status: {}", order.getUserId(), order.getOrderStatus());
        });

        // 4. 전체 결과 로그
        log.info("Final stock: {}", updatedStock.getRemainingStock());
        log.info("Total processed orders: {}", orders.size());
        log.info("Number of unique users: {}", orders.stream().map(Order::getUserId).distinct().count());
        log.info("All orders canceled: {}", orders.stream()
                .allMatch(order -> order.getOrderStatus() == OrderStatusType.CANCELED));

        executorService.shutdown();
    }
}
