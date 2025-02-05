package kr.hhplus.be.server.interfaces.coupon;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.usecase.CouponScheduler;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
public class CouponRedisIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(CouponRedisIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CouponScheduler couponScheduler;

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
        // Redis 데이터 초기화
        redisTemplate.delete(redisTemplate.keys("*"));

        // 테스트용 쿠폰 생성
        Coupon coupon = Coupon.builder()
                .couponName("1000원 할인 쿠폰")
                .discountAmount(1000)
                .originalQuantity(10)  // 테스트를 위해 수량을 10개로 제한
                .remainingQuantity(10)
                .dueDate(LocalDate.now().plusDays(30))
                .build();
        couponRepository.save(coupon);
        savedCouponId = coupon.getId();
    }

    @Test
    @DisplayName("동시에 100명이 쿠폰을 신청해도 10개만 발급되어야 한다")
    public void concurrent_coupon_request() throws Exception {
        // given
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // when: 100명이 동시에 요청
        for (int i = 0; i < numberOfThreads; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/coupons/{userId}/{couponId}", userId, savedCouponId)
                            .contentType(MediaType.APPLICATION_JSON));
                } catch (Exception e) {
                    log.error("Error during coupon request", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 요청이 완료될 때까지 대기
        latch.await(10, TimeUnit.SECONDS);

        // 쿠폰 발급 처리 실행
        Thread.sleep(1000); // Redis에 데이터가 모두 쓰여지기를 기다림
        couponScheduler.issueCoupons();
        Thread.sleep(1000); // 쿠폰 발급 처리가 완료되기를 기다림

        // then
        Coupon coupon = couponRepository.getCoupon(savedCouponId)
                .orElseThrow(() -> new IllegalStateException("Coupon not found"));

        // 1. 잔여 수량이 0이어야 함
        assertThat(coupon.getRemainingQuantity()).isEqualTo(0);

        // 2. 발급된 쿠폰이 정확히 10개여야 함
        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findAllByCouponId(savedCouponId);
        assertThat(issuedCoupons).hasSize(10);

        // 3. 발급된 쿠폰의 사용자 ID가 모두 달라야 함
        Set<Long> userIds = issuedCoupons.stream()
                .map(IssuedCoupon::getUserId)
                .collect(Collectors.toSet());
        assertThat(userIds).hasSize(10);

        // 4. Redis의 요청 데이터가 모두 처리되어야 함
        String requestKey = String.format("coupon-%d-requests", savedCouponId);
        Long remainingRequests = redisTemplate.opsForZSet().size(requestKey);
        assertThat(remainingRequests).isEqualTo(0L);

        executorService.shutdown();
    }
}
