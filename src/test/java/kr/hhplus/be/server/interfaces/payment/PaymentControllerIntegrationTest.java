package kr.hhplus.be.server.interfaces.payment;

import kr.hhplus.be.server.application.event.OrderEventSender;
import kr.hhplus.be.server.application.event.OrderSendEvent;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
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

    private Product product;
    private Stock stock;

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
    void setUp(){
        product = productRepository.save(Product.builder()
                .productName("쿠키")
                .price(5000)
                .build());

        stock = stockRepository.save(Stock.builder()
                .product(product)
                .originStock(10)
                .remainingStock(8)
                .build());
    }



    @Test
    @DisplayName("결제 요청 API - 결제 완료 성공한다.")
    @Transactional
    void createPayment_Success() throws Exception {
        // given
        Order order = Order.builder()
                .userId(1L)
                .originalAmount(10000)
                .finalAmount(10000)
                .discountAmount(0)
                .orderStatus(OrderStatusType.PENDING)
                .build();
        orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(2)
                .productPrice(product.getPrice())
                .totalPrice(product.getPrice() * 2)
                .build();
        orderItemRepository.saveAll(List.of(orderItem));

        Balance balance = Balance.builder()
                .userId(1L)
                .balance(20000)
                .build();
        balanceRepository.save(balance);

        PaymentRequest request = new PaymentRequest(1L, order.getId(), 10000);
        doNothing().when(orderEventSender).send(any(OrderSendEvent.class));

        // when
        ResultActions result = mockMvc.perform(
                post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").exists())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.amount").value(10000))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andDo(print());
        verifyPaymentStatus(order.getId());
        verifyOrderStatus(order.getId());
        Balance updatedBalance = balanceRepository.findById(balance.getId()).orElseThrow();
        assertThat(updatedBalance.getBalance()).isEqualTo(10000);
    }


    @Test
    @DisplayName("결제 요청 API - 잔액 부족 시 IllegalStateException 예외가 발생한다.")
    void createPayment_Insufficient_Balance() throws Exception {
        // given
        Product product = Product.builder()
                .productName("쿠키")
                .price(5000)
                .build();
        productRepository.save(product);

        Stock stock = Stock.builder()
                .product(product)
                .originStock(10)
                .remainingStock(8)
                .build();
        stockRepository.save(stock);

        Order order = Order.builder()
                .userId(2L)
                .originalAmount(10000)
                .finalAmount(10000)
                .discountAmount(0)
                .orderStatus(OrderStatusType.PENDING)
                .build();
        orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(2)
                .productPrice(product.getPrice())
                .totalPrice(product.getPrice() * 2)
                .build();
        orderItemRepository.saveAll(List.of(orderItem));

        Balance balance = Balance.builder()
                .userId(2L)
                .balance(5000)
                .build();
        balanceRepository.save(balance); // 잔액부족

        PaymentRequest request = new PaymentRequest(2L, order.getId(), 10000);


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
//        verifyOrderCanceledStatus(order.getId()); //LazyInitializationException => @Transactional 붙이면 재고 정보 없음 오류
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

    private void verifyRestoreStock(Long stockId) {
        Stock updatedStock = stockRepository.findById(stockId).orElseThrow();
        assertThat(updatedStock.getRemainingStock()).isEqualTo(10);
    }
}
