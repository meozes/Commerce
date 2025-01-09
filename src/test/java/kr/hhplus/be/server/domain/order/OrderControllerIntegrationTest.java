package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.usecase.CouponService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.domain.product.usecase.ProductService;
import kr.hhplus.be.server.interfaces.order.request.OrderItemRequest;
import kr.hhplus.be.server.interfaces.order.request.OrderRequest;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class OrderControllerIntegrationTest {
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

    @Autowired
    private CouponService couponService;

    private Product cookie;
    private Product milk;
    private Stock cookieStock;
    private Stock milkStock;
    private Coupon newCoupon;
    private Coupon expired;
    private Coupon used;
    private IssuedCoupon couponFor1;  // 새로운 쿠폰
    private IssuedCoupon couponFor3;  // 만료된 쿠폰
    private IssuedCoupon couponFor4;  // 사용된 쿠폰


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
        Product product = Product.builder()
                .productName("쿠키")
                .price(10000)
                .build();
        this.cookie = productRepository.save(product);

        Product product2 = Product.builder()
                .productName("비싼 우유")
                .price(50000)
                .build();
        this.milk = productRepository.save(product2);

        Stock stock = Stock.builder()
                .product(product)
                .originStock(100)
                .remainingStock(100)
                .build();
        this.cookieStock = stockRepository.save(stock);

        Stock stock2 = Stock.builder()
                .product(product2)
                .originStock(5)
                .remainingStock(5)
                .build();
        this.milkStock = stockRepository.save(stock2);

        Coupon coupon = Coupon.builder()
                .couponName("1000원 할인 쿠폰")
                .discountAmount(1000)
                .originalQuantity(100)
                .remainingQuantity(90)
                .dueDate(LocalDate.parse("2026-12-31"))
                .build();
        this.newCoupon = couponRepository.save(coupon);

        Coupon expiredCoupon = Coupon.builder()
                .couponName("만료된 쿠폰")
                .discountAmount(1000)
                .originalQuantity(100)
                .remainingQuantity(90)
                .dueDate(LocalDate.now().minusDays(1))
                .build();
        this.expired = couponRepository.save(expiredCoupon);

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(1L)
                .coupon(coupon)
                .couponStatus(CouponStatusType.NEW)
                .issuedAt(LocalDateTime.now())
                .build();
        this.couponFor1 = issuedCouponRepository.save(issuedCoupon);

        IssuedCoupon expiredIssuedCoupon = IssuedCoupon.builder()
                .userId(3L)
                .coupon(expiredCoupon)
                .couponStatus(CouponStatusType.NEW)
                .issuedAt(LocalDateTime.now().minusDays(10))
                .build();
        this.couponFor3 = issuedCouponRepository.save(expiredIssuedCoupon);

        Coupon usedCoupon = Coupon.builder()
                .couponName("사용된 쿠폰")
                .discountAmount(1000)
                .originalQuantity(100)
                .remainingQuantity(90)
                .dueDate(LocalDate.now().plusDays(7))
                .build();
        this.used = couponRepository.save(usedCoupon);

        IssuedCoupon usedIssuedCoupon = IssuedCoupon.builder()
                .userId(4L)
                .coupon(usedCoupon)
                .couponStatus(CouponStatusType.USED)
                .usedAt(LocalDateTime.now().minusDays(1))
                .build();
        this.couponFor4 = issuedCouponRepository.save(usedIssuedCoupon);

        Balance balance = Balance.builder()
                .userId(1L)
                .balance(50000)
                .build();
        balanceRepository.save(balance);

        Balance balance2 = Balance.builder()
                .userId(2L)
                .balance(10000)
                .build();
        balanceRepository.save(balance2);
    }


    @Test
    @DisplayName("주문 생성 API - 주문 생성이 성공한다.")
    void createOrderSuccess() throws Exception {
        // given
        Long userId = 1L;

        OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                .productId(cookie.getId())
                .productName("쿠키")
                .quantity(2)
                .price(10000)
                .build();

        OrderRequest orderRequest = OrderRequest.builder()
                .userId(userId)
                .items(List.of(orderItemRequest))
                .couponId(couponFor1.getId())
                .build();

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.originTotalAmount").value(20000))
                .andExpect(jsonPath("$.data.discountAmount").value(1000))
                .andExpect(jsonPath("$.data.finalAmount").value(19000))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.orderItems[0].productId").value(cookie.getId()))
                .andExpect(jsonPath("$.data.orderItems[0].productName").value("쿠키"))
                .andExpect(jsonPath("$.data.orderItems[0].quantity").value(2))
                .andExpect(jsonPath("$.data.orderItems[0].price").value(10000))
                .andDo(print());

        // DB 저장 검증
        List<Order> savedOrders = orderRepository.findAll();
        assertThat(savedOrders).hasSize(1);
        Order savedOrder = savedOrders.get(0);
        assertThat(savedOrder.getUserId()).isEqualTo(userId);
        assertThat(savedOrder.getOriginalAmount()).isEqualTo(20000);
        assertThat(savedOrder.getDiscountAmount()).isEqualTo(1000);
        assertThat(savedOrder.getFinalAmount()).isEqualTo(19000);

        List<OrderItem> savedOrderItems = orderItemRepository.findAll();
        assertThat(savedOrderItems).hasSize(1);
        OrderItem savedOrderItem = savedOrderItems.get(0);
        assertThat(savedOrderItem.getProductId()).isEqualTo(cookie.getId());
        assertThat(savedOrderItem.getQuantity()).isEqualTo(2);
    }


    @Test
    @DisplayName("주문 생성 API - 재고 부족 시 주문이 실패한다.")
    void createOrderFailWhenInsufficientStock() throws Exception {
        // given
        Long userId = 2L;

        OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                .productId(milk.getId())
                .productName("비싼 우유")
                .quantity(20)  // 재고보다 많은 수량
                .price(50000)
                .build();

        OrderRequest orderRequest = OrderRequest.builder()
                .userId(userId)
                .items(List.of(orderItemRequest))
                .build();

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
        );

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("재고가 부족합니다")))
                .andDo(print());

        List<Order> savedOrders = orderRepository.findAll();
        assertThat(savedOrders).isEmpty();
    }

    @Test
    @DisplayName("주문 생성 API - 존재하지 않는 쿠폰으로 주문 시도 시 실패한다.")
    void createOrderFailWithInvalidCoupon() throws Exception {
        // given
        Long userId = 1L;
        Long invalidCouponId = 999L;

        OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                .productId(cookie.getId())
                .productName("쿠키")
                .quantity(2)
                .price(10000)
                .build();

        OrderRequest orderRequest = OrderRequest.builder()
                .userId(userId)
                .items(List.of(orderItemRequest))
                .couponId(invalidCouponId)
                .build();

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
        );

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 쿠폰이 존재하지 않습니다."))
                .andDo(print());

        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("주문 생성 API - 만료된 쿠폰으로 주문 시도 시 실패한다.")
    void createOrderFailWithExpiredCoupon() throws Exception {
        // given
        Long userId = 3L;

        OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                .productId(cookie.getId())
                .productName("쿠키")
                .quantity(2)
                .price(10000)
                .build();

        OrderRequest orderRequest = OrderRequest.builder()
                .userId(userId)
                .items(List.of(orderItemRequest))
                .couponId(couponFor3.getId())
                .build();

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
        );

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("만료된 쿠폰입니다")))
                .andDo(print());

        assertThat(orderRepository.findAll()).isEmpty();

    }

    @Test
    @DisplayName("주문 생성 API - 이미 사용된 쿠폰으로 주문 시도 시 실패한다.")
    void createOrderFailWithUsedCoupon() throws Exception {
        // given
        Long userId = 4L;

        OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                .productId(cookie.getId())
                .productName("쿠키")
                .quantity(2)
                .price(10000)
                .build();

        OrderRequest orderRequest = OrderRequest.builder()
                .userId(userId)
                .items(List.of(orderItemRequest))
                .couponId(couponFor4.getId())
                .build();

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
        );

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("이미 사용된 쿠폰입니다")))
                .andDo(print());

        assertThat(orderRepository.findAll()).isEmpty();
    }

}
