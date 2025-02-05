package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.usecase.CouponScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouponSchedulerTest {
    @InjectMocks
    private CouponScheduler scheduler;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("쿠폰 발급 처리 - 성공")
    void issueCoupons_success() {
        // given
        Set<String> requestKeys = Set.of("coupon-1-requests", "coupon-2-requests");
        when(redisTemplate.keys(anyString())).thenReturn(requestKeys);

        List<Coupon> coupons = List.of(
                createCoupon(1L, 10),
                createCoupon(2L, 5)
        );
        when(couponRepository.findAvailableCoupons(anyList(), any(LocalDate.class))).thenReturn(coupons);

        Set<ZSetOperations.TypedTuple<String>> requests = Set.of(
                createTypedTuple("1", 100.0),
                createTypedTuple("2", 200.0)
        );
        when(zSetOperations.popMin(anyString(), anyLong())).thenReturn(requests);

        // when
        scheduler.issueCoupons();

        // then
        verify(couponRepository, times(2)).save(any(Coupon.class));
        verify(issuedCouponRepository, times(2)).saveAll(anyList());
        verify(setOperations, times(2)).add(anyString(), any(String[].class));
    }

    private Coupon createCoupon(Long id, int quantity) {
        return Coupon.builder()
                .id(id)
                .remainingQuantity(quantity)
                .dueDate(LocalDate.now().plusDays(7))
                .build();
    }

    private ZSetOperations.TypedTuple<String> createTypedTuple(String value, Double score) {
        return new DefaultTypedTuple<>(value, score);
    }
}
