package kr.hhplus.be.server.interfaces.balance;


import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import kr.hhplus.be.server.interfaces.balance.request.ChargeRequest;
import kr.hhplus.be.server.interfaces.common.response.ApiResponse;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import org.junit.jupiter.api.*;
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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BalanceChargeOptimisticLockIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BalanceRepository balanceRepository;

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
        Balance initialBalance = Balance.builder()
                .userId(78L)
                .balance(0)
                .build();
        balanceRepository.save(initialBalance);
    }

    @Test
    @DisplayName("동일 유저가 동시에 여러번 잔고 충전 시도 시 요청은 한번만 처리되고 OptimisticLockingFailureException이 발생한다.")
    void chargeSeveralTimes_WithOptimisticLock() throws Exception {

        Long userId = 78L;
        int numberOfThreads = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        int chargeAmount = 10000;
        int expectedBalance = 10000;

        List<Future<ResultActions>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    startLatch.await();
                    ChargeRequest request = new ChargeRequest(userId, chargeAmount);
                    return mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/api/balance/charge")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    );
                } finally {
                    endLatch.countDown();
                }
            }));
        }

        startLatch.countDown();
        // 모든 요청이 완료될 때까지 대기
        endLatch.await(10, TimeUnit.SECONDS);

        // then
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger optimisticLockErrorCount = new AtomicInteger(0);

        for (Future<ResultActions> future : futures) {
            try {
                MvcResult result = future.get().andReturn();
                String content = result.getResponse().getContentAsString();

                if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
                    successCount.incrementAndGet();
                } else if (result.getResponse().getStatus() == HttpStatus.CONFLICT.value()) {
                    optimisticLockErrorCount.incrementAndGet();
                    assertTrue(content.contains(ErrorCode.ALREADY_CHARGED.getMessage()));
                }
            } catch (Exception e) {
                // 실패한 요청 카운트
                optimisticLockErrorCount.incrementAndGet();
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, successCount.get(), "성공한 요청은 1개여야 합니다");
        assertEquals(numberOfThreads - 1, optimisticLockErrorCount.get(), "나머지는 모두 충돌 에러여야 합니다");

        Balance balance = balanceRepository.getBalance(userId).orElseThrow();
        assertEquals(balance.getBalance(), expectedBalance);

    }

}
