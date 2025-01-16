package kr.hhplus.be.server.interfaces.balance;

import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
import kr.hhplus.be.server.interfaces.balance.request.ChargeRequest;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import kr.hhplus.be.server.interfaces.payment.request.PaymentRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BalanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private OrderRepository orderRepository;

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
    @DisplayName("잔고 조회 API - 신규 사용자의 경우 0원을 반환한다")
    void getBalance_NewUser() throws Exception {
        // given
        Long userId = 9L;

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/balance/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.amount").value(0))
                .andDo(print());
    }

    @Test
    @DisplayName("잔고 조회 API - 기존 사용자의 경우 저장된 잔고를 반환한다")
    void getBalance_ExistingUser() throws Exception {
        // given
        Long userId = 2L;
        Integer initialBalance = 10000;
        Balance balance = Balance.builder()
                .userId(userId)
                .balance(initialBalance)
                .build();
        balanceRepository.save(balance);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/balance/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.amount").value(initialBalance))
                .andDo(print());
    }

    @Test
    @DisplayName("잔고 충전 API - 정상적인 충전 요청시 잔고가 증가한다")
    void chargeBalance_ValidRequest() throws Exception {
        // given
        Long userId = 22L;
        Integer chargeAmount = 10000;
        ChargeRequest request = new ChargeRequest(userId, chargeAmount);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.amount").value(10000))  // 총 잔고
                .andExpect(jsonPath("$.data.chargedAmount").value(chargeAmount))  // 충전된 금액
                .andDo(print());

        Balance savedBalance = balanceRepository.getBalance(userId).orElseThrow();
        assertThat(savedBalance.getBalance()).isEqualTo(chargeAmount);
    }

    @Test
    @DisplayName("잔고 충전 API - 유효하지 않은 금액으로 충전 시도하면 IllegalArgumentException 예외가 발생한다")
    void chargeBalance_InvalidAmount() throws Exception {
        // given
        Long userId = 1L;
        Integer chargeAmount = -10000;
        ChargeRequest request = new ChargeRequest(userId, chargeAmount);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_AMOUNT_INPUT.getMessage()))
                .andDo(print());
    }

    @Test
    @DisplayName("잔고 충전 API - 기존 잔고가 있는 사용자 충전시 잔고가 합산된다")
    void chargeBalance_ExistingBalance() throws Exception {
        // given
        Long userId = 11L;
        Integer initialBalance = 10000;
        Balance balance = Balance.builder()
                .userId(userId)
                .balance(initialBalance)
                .build();
        balanceRepository.save(balance);

        Integer chargeAmount = 5000;
        ChargeRequest request = new ChargeRequest(userId, chargeAmount);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/balance/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.amount").value(initialBalance + chargeAmount))
                .andDo(print());

        Balance savedBalance = balanceRepository.getBalance(userId).orElseThrow();
        assertThat(savedBalance.getBalance()).isEqualTo(initialBalance + chargeAmount);
    }

    @Test
    @DisplayName("잔고 충전 API - 동시성 테스트. 동시에 여러 충전 요청이 들어와도 정확한 금액이 충전된다")
    void chargeBalance_Concurrently() throws Exception {
        // given
        Long userId = 33L;
        Integer initialBalance = 1000;
        Balance balance = Balance.builder()
                .userId(userId)
                .balance(initialBalance)
                .build();
        balanceRepository.save(balance);

        int numberOfThreads = 3; // 락 타임아웃(3초)을 고려하여 스레드 수 조정
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        Integer chargeAmount = 1000;

        // when
        List<Future<ResultActions>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    ChargeRequest request = new ChargeRequest(userId, chargeAmount);
                    return mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/api/balance/charge")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    );
                } finally {
                    latch.countDown();
                }
            }));
        }

        // then
        latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // 모든 요청이 성공했는지 확인
        for (Future<ResultActions> future : futures) {
            future.get()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.status").value("OK"))
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.chargedAmount").value(chargeAmount));
        }

        // DB에서 최종 잔고 확인
        Balance finalBalance = balanceRepository.getBalance(userId).orElseThrow();
        Integer expectedBalance = initialBalance + (chargeAmount * numberOfThreads);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);

        // API로 최종 잔고 다시 확인
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/balance/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.amount").value(expectedBalance))
                .andDo(print());
    }

    @Test
    @DisplayName("잔고 충전 API - 동시성 테스트. 동시에 충전과 사용 요청이 들어와도 정확한 금액이 계산된다.")
    void chargeAndUseBalance_Concurrently() throws Exception {
        Long userId = 77L;
        Integer initialBalance = 100000;
        Balance balance = Balance.builder()
                .userId(userId)
                .balance(initialBalance)
                .build();
        balanceRepository.save(balance);

        Order order = Order.builder()
                .id(1L)
                .userId(userId)
                .originalAmount(5000)
                .finalAmount(5000)
                .discountAmount(0)
                .orderStatus(OrderStatusType.PENDING)
                .build();
        orderRepository.save(order);

        int chargeNumberOfThreads = 3;
        int payNumberOfThread = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(chargeNumberOfThreads);
        ExecutorService executorService2 = Executors.newFixedThreadPool(payNumberOfThread);
        CountDownLatch latch = new CountDownLatch(chargeNumberOfThreads * 2);
        CountDownLatch latch2 = new CountDownLatch(payNumberOfThread * 2);
        Integer chargeAmount = 50000;
        Integer useAmount = 5000;

        List<Future<ResultActions>> chargeFutures = new ArrayList<>();
        List<Future<ResultActions>> paymentFutures = new ArrayList<>();

        // 충전 요청
        for (int i = 0; i < chargeNumberOfThreads; i++) {
            chargeFutures.add(executorService.submit(() -> {
                try {
                    ChargeRequest request = new ChargeRequest(userId, chargeAmount);
                    return mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/api/balance/charge")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    );
                } finally {
                    latch.countDown();
                }
            }));
        }

        // 결제 요청
        for (int i = 0; i < payNumberOfThread; i++) {
            paymentFutures.add(executorService.submit(() -> {
                try {
                    PaymentRequest paymentRequest = new PaymentRequest(userId, order.getId(), useAmount);
                    return mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/api/payments")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(paymentRequest))
                    );
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await(10, TimeUnit.SECONDS);
        latch2.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService2.shutdown();

        for (Future<ResultActions> future : chargeFutures) {
            future.get()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.status").value("OK"))
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.chargedAmount").value(chargeAmount));
        }

        for (Future<ResultActions> future : paymentFutures) {
            future.get()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.status").value("OK"))
                    .andExpect(jsonPath("$.data.orderId").value(order.getId()))
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        // DB에서 최종 잔고 확인
        Balance finalBalance = balanceRepository.getBalance(userId).orElseThrow();
        Integer expectedBalance = initialBalance + (chargeAmount * chargeNumberOfThreads) - (useAmount * payNumberOfThread);
        assertThat(finalBalance.getBalance()).isEqualTo(expectedBalance);

    }

}
