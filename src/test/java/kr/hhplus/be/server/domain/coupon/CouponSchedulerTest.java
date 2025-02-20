package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
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
import org.springframework.data.redis.core.*;

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

    @Mock
    private BoundZSetOperations<String, String> boundZSetOps;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.boundZSetOps(anyString())).thenReturn(boundZSetOps);
    }

    @Test
    @DisplayName("쿠폰 발급 처리 - 성공")
    void issueCoupons_success() {
        // given
        Set<String> requestKeys = Set.of("coupon-1-requests", "coupon-2-requests");
        when(redisTemplate.keys(anyString())).thenReturn(requestKeys);

        Set<ZSetOperations.TypedTuple<String>> typedTuples = Set.of(
                createTypedTuple("1", 100.0),
                createTypedTuple("2", 200.0)
        );
        when(zSetOperations.rangeWithScores(anyString(), anyLong(), anyLong()))
                .thenReturn(typedTuples);

        List<Coupon> coupons = List.of(
                createCoupon(1L, 2),  // remainingQuantity를 2로 수정
                createCoupon(2L, 2)   // remainingQuantity를 2로 수정
        );
        when(couponRepository.findAvailableCoupons(anyList(), any(LocalDate.class)))
                .thenReturn(coupons);

        // boundZSetOps 동작 설정
        when(boundZSetOps.size()).thenReturn(2L);

        // 각 쿠폰당 2개의 요청을 처리하도록 설정
        when(boundZSetOps.popMin(1))
                .thenReturn(Set.of(createTypedTuple("1", 100.0)))
                .thenReturn(Set.of(createTypedTuple("2", 200.0)))
                .thenReturn(Set.of(createTypedTuple("3", 300.0)))
                .thenReturn(Set.of(createTypedTuple("4", 400.0)));

        // when
        scheduler.issueCoupons();

        // then
        verify(couponRepository, times(2)).save(any(Coupon.class));
        verify(issuedCouponRepository, times(4)).save(any(IssuedCoupon.class));
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
