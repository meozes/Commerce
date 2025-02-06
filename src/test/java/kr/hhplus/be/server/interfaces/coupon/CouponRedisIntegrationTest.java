package kr.hhplus.be.server.interfaces.coupon;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.usecase.CouponScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.Commit;
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
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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
    @Qualifier("redisTemplate")
    private RedisTemplate<String, String> redisTemplate;


    @TestConfiguration
    static class TestConfig {
        @Bean
        public TaskScheduler taskScheduler() {
            return new ThreadPoolTaskScheduler();
        }
    }

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


    @Test
    @DisplayName("동시에 100명이 쿠폰을 신청해도 10개만 발급되어야 한다")
    public void concurrent_coupon_request() throws Exception {
        // given

        Coupon origin = Coupon.builder()
                .couponName("1000원 할인 쿠폰")
                .discountAmount(1000)
                .originalQuantity(10)  // 테스트를 위해 수량을 10개로 제한
                .remainingQuantity(10)
                .dueDate(LocalDate.now().plusDays(30))
                .build();
        couponRepository.save(origin);
        couponRepository.flush();
        log.info("Saved coupon: {}", origin);
        savedCouponId = origin.getId();

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // when: 100명이 동시에 요청
        for (int i = 0; i < numberOfThreads; i++) {
            long userId = i + 1;
            executorService.submit(() -> {
                try {
                    mockMvc.perform(
                            MockMvcRequestBuilders
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

        List<Coupon> beforeCoupons = couponRepository.findAvailableCoupons(
                Collections.singletonList(savedCouponId),
                LocalDate.now()
        );
        log.info("발급 가능 쿠폰: couponId = {}, couponName = {}",
                beforeCoupons.get(0).getId(),
                beforeCoupons.get(0).getCouponName(),
                beforeCoupons.get(0).getRemainingQuantity(),
                beforeCoupons.get(0).getDueDate());

        // 쿠폰 발급 처리 실행
        Thread.sleep(2000); // Redis에 데이터가 모두 쓰여지기를 기다림
        couponScheduler.issueCoupons();
        Thread.sleep(2000); // 쿠폰 발급 처리가 완료되기를 기다림

        // then
        Coupon coupon = couponRepository.getCoupon(savedCouponId)
                .orElseThrow(() -> new IllegalStateException("Coupon not found"));
        log.info("쿠폰 결과: couponId = {}, remainingQuantity = {}", coupon.getId(), coupon.getRemainingQuantity());

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

        executorService.shutdown();
    }
}
