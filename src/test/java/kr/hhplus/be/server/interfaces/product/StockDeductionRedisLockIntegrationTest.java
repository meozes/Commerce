package kr.hhplus.be.server.interfaces.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.interfaces.order.request.OrderItemRequest;
import kr.hhplus.be.server.interfaces.order.request.OrderRequest;
import kr.hhplus.be.server.domain.order.entity.Order;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class StockDeductionRedisLockIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(StockDeductionRedisLockIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private BalanceRepository balanceRepository;



    private Product cookie;
    private final int ORDER_QUANTITY_PER_USER = 3;
    private final int INITIAL_STOCK = 100;

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
    

    @BeforeEach
    void setUp() {
         cookie = productRepository.save(Product.builder()
                .productName("쿠키")
                .price(10000)
                .build());

        stockRepository.save(Stock.builder()
                .product( cookie)
                .originStock(INITIAL_STOCK)
                .remainingStock(INITIAL_STOCK)
                .build());

    }

    @Test
    @DisplayName("Redisson 분산 락을 통한 동시성 재고 차감 테스트 - 정상 케이스")
    void stockDeduction_WithRLock() throws Exception {
        // given
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger();

        List<Long> userIds = new ArrayList<>();
        for (long i = 50; i < 60; i++) {
            userIds.add(i);
        }

        for (Long userId : userIds) {
            balanceRepository.save(Balance.builder()
                    .userId(userId)
                    .balance(50000)
                    .build());
        }

        // when
        for (Long userId : userIds) {
            executorService.submit(() -> {
                try {
                    OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                            .productId( cookie.getId())
                            .productName( cookie.getProductName())
                            .quantity(ORDER_QUANTITY_PER_USER)
                            .price( cookie.getPrice())
                            .build();

                    OrderRequest orderRequest = OrderRequest.builder()
                            .userId(userId)
                            .items(List.of(orderItemRequest))
                            .build();

                    ResultActions resultActions = mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(orderRequest)));

                    MvcResult result = resultActions
                            .andExpect(status().isOk())
                            .andReturn();

                    if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
                        successCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    log.info("주문 처리 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 요청이 완료될 때까지 대기
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(completed).isTrue().as("모든 요청이 제한 시간 내에 완료되어야 합니다");

        // then
        Stock finalStock = stockRepository.findById( cookie.getId())
                .orElseThrow(() -> new NoSuchElementException("재고를 찾을 수 없습니다"));

        // 성공한 주문 수만큼 재고가 차감되었는지 확인
        assertThat(finalStock.getRemainingStock())
                .isEqualTo(INITIAL_STOCK - (ORDER_QUANTITY_PER_USER * successCount.get()));

        // 실제 생성된 주문 수 확인
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(successCount.get());

    }

    @Test
    @DisplayName("Redisson 분산 락을 통한 동시성 재고 차감 테스트 - 여러 상품 동시 주문")
    void multiStockDeduction_WithRLock() throws Exception {
        // given
        Product  cookie2 = productRepository.save(Product.builder()
                .productName("사탕")
                .price(5000)
                .build());

        stockRepository.save(Stock.builder()
                .product( cookie2)
                .originStock(INITIAL_STOCK)
                .remainingStock(INITIAL_STOCK)
                .build());

        int numberOfThreads = 10;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger();

        List<Long> userIds = new ArrayList<>();
        for (long i = 60; i < 70; i++) {
            userIds.add(i);
        }

        for (Long userId : userIds) {
            balanceRepository.save(Balance.builder()
                    .userId(userId)
                    .balance(50000)
                    .build());
        }

        // when
        for (Long userId : userIds) {
            executorService.submit(() -> {
                try {
                    List<OrderItemRequest> orderItems = List.of(
                            OrderItemRequest.builder()
                                    .productId( cookie.getId())
                                    .productName( cookie.getProductName())
                                    .quantity(ORDER_QUANTITY_PER_USER)
                                    .price( cookie.getPrice())
                                    .build(),
                            OrderItemRequest.builder()
                                    .productId( cookie2.getId())
                                    .productName( cookie2.getProductName())
                                    .quantity(ORDER_QUANTITY_PER_USER)
                                    .price( cookie2.getPrice())
                                    .build()
                    );

                    OrderRequest orderRequest = OrderRequest.builder()
                            .userId(userId)
                            .items(orderItems)
                            .build();

                    MvcResult result = mockMvc.perform(
                                    MockMvcRequestBuilders
                                            .post("/api/orders")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(orderRequest)))
                            .andExpect(status().isOk())
                            .andReturn();

                    if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
                        successCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    log.info("주문 처리 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 요청이 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        Stock finalStock1 = stockRepository.findById( cookie.getId())
                .orElseThrow(() -> new NoSuchElementException("재고를 찾을 수 없습니다"));
        Stock finalStock2 = stockRepository.findById( cookie2.getId())
                .orElseThrow(() -> new NoSuchElementException("재고를 찾을 수 없습니다"));

        // 각 상품의 재고가 정확히 차감되었는지 확인
        assertThat(finalStock1.getRemainingStock())
                .isEqualTo(INITIAL_STOCK - (ORDER_QUANTITY_PER_USER * successCount.get()));
        assertThat(finalStock2.getRemainingStock())
                .isEqualTo(INITIAL_STOCK - (ORDER_QUANTITY_PER_USER * successCount.get()));

        // 실제 생성된 주문 수 확인
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(successCount.get());

    }


}
