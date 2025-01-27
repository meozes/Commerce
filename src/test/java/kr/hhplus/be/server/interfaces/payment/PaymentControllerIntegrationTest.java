package kr.hhplus.be.server.interfaces.payment;

import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.interfaces.external.OrderEventSender;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.domain.product.usecase.StockService;
import kr.hhplus.be.server.interfaces.payment.request.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Mock
    private OrderEventSender orderEventSender;

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


    @Test
    @DisplayName("결제 요청 API - 결제 완료 성공한다.")
    @Transactional
    void createPayment_Success() throws Exception {
        // given
        // 1. 상품과 재고 설정
        Product product = createAndSaveProduct();
        Stock stock = createAndSaveStock(product);

        // 2. 주문과 주문상품 설정
        Order order = createAndSaveOrder();
        OrderItem orderItem = createAndSaveOrderItem(order, product);

        // 3. 잔액 설정
        Balance balance = createAndSaveBalance();

        // 4. 결제 요청 데이터 설정
        PaymentRequest request = new PaymentRequest(111L, order.getId(), 10000);
        doNothing().when(orderEventSender).send(any(Order.class));

        // when
        ResultActions result = mockMvc.perform(
                post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // then
        verifyResponse(result);
        verifyPaymentStatus(order.getId());
        verifyOrderStatus(order.getId());
        verifyBalanceDeducted(balance.getId());
    }

    private Product createAndSaveProduct() {
        Product product = Product.builder()
                .productName("쿠키")
                .price(5000)
                .build();
        return productRepository.save(product);
    }

    private Stock createAndSaveStock(Product product) {
        Stock stock = Stock.builder()
                .product(product)
                .originStock(10)
                .remainingStock(8)
                .build();
        return stockRepository.save(stock);
    }

    private Order createAndSaveOrder() {
        Order order = Order.builder()
                .userId(1L)
                .originalAmount(10000)
                .finalAmount(10000)
                .discountAmount(0)
                .orderStatus(OrderStatusType.PENDING)
                .build();
        return orderRepository.save(order);
    }

    private OrderItem createAndSaveOrderItem(Order order, Product product) {
        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(2)
                .productPrice(product.getPrice())
                .totalPrice(product.getPrice() * 2)
                .build();
        orderItemRepository.saveAll(List.of(orderItem));
        return orderItem;
    }

    private Balance createAndSaveBalance() {
        Balance balance = Balance.builder()
                .userId(111L)
                .balance(20000)
                .build();
        return balanceRepository.save(balance);
    }

    private void verifyResponse(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").exists())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.amount").value(10000))
                .andExpect(jsonPath("$.data.userId").value(111L))
                .andDo(print());
    }

    private void verifyPaymentStatus(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusType.COMPLETED);
    }

    private void verifyOrderStatus(Long orderId) {
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatusType.COMPLETED);
    }

    private void verifyOrderCanceledStatus(Long orderId) {
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatusType.CANCELED);
    }

    private void verifyBalanceDeducted(Long balanceId) {
        Balance updatedBalance = balanceRepository.findById(balanceId).orElseThrow();
        assertThat(updatedBalance.getBalance()).isEqualTo(10000);
    }

    private void verifyRestoreStock(Long stockId) {
        Stock updatedStock = stockRepository.findById(stockId).orElseThrow();
        assertThat(updatedStock.getRemainingStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("결제 요청 API - 잔액 부족 시 IllegalStateException 예외가 발생한다.")
    void createPayment_Insufficient_Balance() throws Exception {
        // given
        // 1. 상품과 재고 설정
        Product product = createAndSaveProduct();
        Stock stock = createAndSaveStock(product);

        // 2. 주문과 주문상품 설정
        Order order = createAndSaveOrder();
        OrderItem orderItem = createAndSaveOrderItem(order, product);

        Balance balance = Balance.builder()
                .userId(1L)
                .balance(5000)
                .build();
        balanceRepository.save(balance); // 잔액부족

        PaymentRequest request = new PaymentRequest(1L, order.getId(), 10000);


        // when
        ResultActions result = mockMvc.perform(
                post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("잔액이 부족합니다.")))
                .andDo(print());

        verifyRestoreStock(stock.getId());
        verifyOrderCanceledStatus(order.getId());
    }
}
