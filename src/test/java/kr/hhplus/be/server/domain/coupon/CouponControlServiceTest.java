package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponIssueRequestInfo;
import kr.hhplus.be.server.domain.coupon.usecase.CouponControlService;
import kr.hhplus.be.server.domain.coupon.validation.CouponValidator;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponControlServiceTest {
    @InjectMocks
    private CouponControlService couponControlService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private CouponValidator couponValidator;

    @Test
    @DisplayName("쿠폰 발급 요청 - 성공")
    void requestCoupon_success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        CouponCommand command = CouponCommand.of(userId, couponId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        doNothing().when(couponValidator).validateUserId(userId);
        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        // when
        CouponIssueRequestInfo result = couponControlService.requestCoupon(command);

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCouponId()).isEqualTo(couponId);
        assertThat(result.getRequestedAt()).isNotNull();

        verify(couponValidator).validateUserId(userId);
        verify(setOperations).isMember(eq("coupon-1-issued"), eq("1"));
        verify(zSetOperations).add(eq("coupon-1-requests"), eq("1"), anyDouble());
    }

    @Test
    @DisplayName("쿠폰 발급 요청 - 이미 발급된 경우")
    void requestCoupon_alreadyIssued() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        CouponCommand command = CouponCommand.of(userId, couponId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(anyString(), anyString())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> couponControlService.requestCoupon(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ErrorCode.COUPON_ALREADY_ISSUED.getMessage());
    }

}
