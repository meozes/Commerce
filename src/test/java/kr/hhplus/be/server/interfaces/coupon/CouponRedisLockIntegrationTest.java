package kr.hhplus.be.server.interfaces.coupon;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.util.NestedServletException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
public class CouponRedisLockIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(CouponRedisLockIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    private Long savedCouponId;

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
        // 테스트 데이터 세팅
        Coupon coupon = Coupon.builder()
                .couponName("1000원 할인 쿠폰")
                .discountAmount(1000)
                .originalQuantity(100)
                .remainingQuantity(100)
                .dueDate(LocalDate.now().plusDays(30))
                .build();
        couponRepository.save(coupon);
        savedCouponId = coupon.getId();

        Long userId = 1L;
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .couponStatus(CouponStatusType.NEW)
                .issuedAt(LocalDateTime.now())
                .build();
        issuedCouponRepository.save(issuedCoupon);
    }

    @Test
    @DisplayName("쿠폰 발급 API - 동시성 테스트. 서로 다른 사용자가 동일한 쿠폰을 동시에 발급 요청하면 정상적으로 처리된다")
    void issueCoupon_Concurrently() throws Exception {
        // given
        int numberOfThreads = 5;
        List<Long> userIds = Arrays.asList(5L, 6L, 7L, 8L, 9L);
        Long couponId = savedCouponId;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);
        List<ResultActions> results = Collections.synchronizedList(new ArrayList<>());

        // when: 동시에 요청 실행
        List<Future<?>> futures = new ArrayList<>();
        for (Long userId : userIds) {
            Future<?> future = executorService.submit(() -> {
                try {
                    barrier.await(); // 모든 스레드가 준비될 때까지 대기
                    ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/coupons/{userId}/{couponId}", userId, couponId)
                            .contentType(MediaType.APPLICATION_JSON));
                    results.add(result);
                    return result;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // 모든 요청이 완료될 때까지 대기
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        // 모든 Future의 결과 확인
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS); // 타임아웃 5초 설정
        }

        // then
        // 1. 모든 요청이 성공했는지 확인
        assertThat(results).hasSize(numberOfThreads);
        for (ResultActions result : results) {
            result.andExpect(status().isOk());
        }

        // 2. 쿠폰 수량이 정확히 차감되었는지 확인
        Thread.sleep(1000); // 트랜잭션 완료 대기
        Optional<Coupon> updatedOptionalCoupon = couponRepository.getCoupon(couponId);
        Coupon updatedCoupon = updatedOptionalCoupon.orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다."));

        // 남은 수량 로깅
        log.info("Final remaining quantity: {}", updatedCoupon.getRemainingQuantity());
        assertThat(updatedCoupon.getRemainingQuantity())
                .isEqualTo(95);

        // 3. 각 사용자별로 쿠폰 발급 여부 확인
        for (Long userId : userIds) {
            Page<IssuedCoupon> issuedCoupons = issuedCouponRepository.getIssuedCoupons(
                    PageRequest.of(0, 10), userId);
            List<IssuedCoupon> coupons = issuedCoupons.getContent();
            coupons.stream()
                    .forEach(coupon -> log.info("IssuedCoupon ID: {}, Coupon ID: {}, User ID: {}",
                            coupon.getId(),
                            coupon.getCoupon().getId(),
                            coupon.getUserId()
                            ));

            // 발급된 쿠폰 정보 로깅
            log.info("User {} issued coupons: {}", userId, coupons.size());
            assertThat(coupons)
                    .as("한명 당 같은 쿠폰은 하나만 발급 가능")
                    .hasSize(1);

            IssuedCoupon issuedCoupon = coupons.get(0);
            log.info("Issued coupon for user {}: id={}, status={}, issuedAt={}",
                    userId, issuedCoupon.getId(), issuedCoupon.getCouponStatus(), issuedCoupon.getIssuedAt());
        }

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();
    }

    @Test
    @DisplayName("쿠폰 발급 API - 동일 사용자가 동일 쿠폰을 동시에 여러 번 요청하면 한 번만 발급된다")
    void issueCoupon_Concurrently_SameUser() throws Exception {
        // given
        int numberOfThreads = 3;
        Long userId = 10L;
        Long couponId = savedCouponId;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);
        List<ResultMatcher> results = Collections.synchronizedList(new ArrayList<>());

        // when: 동시에 요청 실행
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    barrier.await(); // 모든 스레드가 준비될 때까지 대기
                    ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                                    .post("/api/coupons/{userId}/{couponId}", userId, couponId)
                                    .contentType(MediaType.APPLICATION_JSON));
                    results.add(status().isOk());
                } catch (NestedServletException e) {
                    if (e.getRootCause() instanceof IllegalStateException) {
                        results.add(status().isConflict());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 요청이 완료될 때까지 대기
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        executorService.shutdown();

        // then: 쿠폰이 한 번만 발급되었는지 확인
        Page<IssuedCoupon> issuedCoupons = issuedCouponRepository.getIssuedCoupons(
                PageRequest.of(0, 10), userId);
        assertThat(issuedCoupons.getContent()).hasSize(1); // 한 명의 사용자당 1개만 발급되어야 함

        Optional<Coupon> updatedOptionalCoupon = couponRepository.getCoupon(couponId);
        Coupon updatedCoupon = updatedOptionalCoupon.orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다."));
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(99); // 100 - 1
    }

}
