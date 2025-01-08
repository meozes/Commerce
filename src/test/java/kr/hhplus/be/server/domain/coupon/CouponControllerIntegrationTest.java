package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class CouponControllerIntegrationTest {
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

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
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
        couponRepository.saveCoupon(coupon);
        savedCouponId = coupon.getId();

        Long userId = 1L;
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .couponStatus(CouponStatusType.NEW)
                .issuedAt(LocalDateTime.now())
                .build();
        issuedCouponRepository.saveIssuedCoupon(issuedCoupon);
    }

    @Test
    @DisplayName("사용자의 쿠폰 목록을 정상적으로 조회한다")
    void getCoupons() throws Exception {
        // given
        Long userId = 1L;

        // when

        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/coupons/{userId}", userId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.content[0].couponName").value("1000원 할인 쿠폰"))
                .andExpect(jsonPath("$.data.content[0].discountAmount").value(1000))
                .andExpect(jsonPath("$.data.content[0].couponStatus").value("NEW"))
                .andDo(print());
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 쿠폰 목록을 조회하면 빈 페이지를 반환한다")
    void getCouponsWithNonExistentUser() throws Exception {
        // given
        Long nonExistentUserId = 999L;

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/coupons/{userId}", nonExistentUserId)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰을 정상적으로 발급한다")
    void issueCoupon() throws Exception {
        // given
        Long userId = 3L;
        Coupon coupon = couponRepository.getCoupon(savedCouponId); // setUp에서 생성한 쿠폰의 ID

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/coupons/{userId}/{couponId}", userId, coupon.getId())
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.couponName").value("1000원 할인 쿠폰"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.discountAmount").value(1000))
                .andExpect(jsonPath("$.data.couponStatus").value("NEW"))
                .andDo(print());

        // DB 검증
        Coupon updatedCoupon = couponRepository.getCoupon(coupon.getId());
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 발급 시 예외가 발생한다")
    void issueCouponWithNonExistentCoupon() throws Exception {
        // given
        Long userId = 1L;
        Long nonExistentCouponId = 999L;

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/coupons/{userId}/{couponId}", userId, nonExistentCouponId)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("잘못된 사용자 ID로 쿠폰 발급 시 예외가 발생한다")
    void issueCouponWithInvalidUserId() throws Exception {
        // given
        Long invalidUserId = -1L;
        Long couponId = 1L; // setUp에서 생성한 쿠폰의 ID

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/coupons/{userId}/{couponId}", invalidUserId, couponId)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("수량이 모두 소진된 쿠폰 발급 시 예외가 발생한다")
    void issueCouponWithNoQuantity() throws Exception {
        // given
        Long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponName("수량 없는 쿠폰")
                .discountAmount(1000)
                .originalQuantity(0)
                .remainingQuantity(0)
                .dueDate(LocalDate.now().plusDays(30))
                .build();
        couponRepository.saveCoupon(coupon);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/coupons/{userId}/{couponId}", userId, coupon.getId())
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("동일한 쿠폰을 동시에 여러 번 발급 요청해도 정상적으로 처리된다")
    void issueCouponConcurrently() throws Exception {
        // given
        int numberOfThreads = 3;
        Long userId = 2L;
        Long couponId = savedCouponId; // setUp에서 생성한 쿠폰의 ID
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/api/coupons/{userId}/{couponId}", userId, couponId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    );
                } catch (Exception e) {
                    // 예외 발생 시 처리
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);

        // then
        Coupon updatedCoupon = couponRepository.getCoupon(couponId);
        assertThat(updatedCoupon.getRemainingQuantity()).isEqualTo(97);

        Page<IssuedCoupon> issuedCoupons = issuedCouponRepository.getIssuedCoupons(
                PageRequest.of(0, 10), userId);
        assertThat(issuedCoupons.getContent()).hasSize(3);
    }




}