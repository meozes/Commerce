package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponSearch;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.usecase.CouponControlService;
import kr.hhplus.be.server.domain.coupon.usecase.CouponFindService;
import kr.hhplus.be.server.domain.coupon.validation.CouponValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import kr.hhplus.be.server.domain.coupon.dto.CouponInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private CouponValidator couponValidator;

    @InjectMocks
    private CouponFindService couponFindService;

    @InjectMocks
    private CouponControlService couponControlService;

    /**
     * 쿠폰 조회 테스트
     */
    @Test
    @DisplayName("쿠폰 조회 성공")
    void getCoupon_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        CouponSearch couponSearch = CouponSearch.of(userId, 0, 10);

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .couponName("10% 할인 쿠폰")
                .discountAmount(1000)
                .build();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .id(1L)
                .userId(userId)
                .coupon(coupon)
                .couponStatus(CouponStatusType.NEW)
                .issuedAt(LocalDateTime.now())
                .build();

        List<IssuedCoupon> issuedCoupons = Collections.singletonList(issuedCoupon);
        Page<IssuedCoupon> issuedCouponPage = new PageImpl<>(issuedCoupons);

        // when
        doNothing().when(couponValidator).validateUserId(userId);
        when(issuedCouponRepository.getIssuedCoupons(any(PageRequest.class), eq(userId)))
                .thenReturn(issuedCouponPage);
        when(couponRepository.getCoupon(eq(couponId)))
                .thenReturn(Optional.ofNullable(coupon));

        Page<CouponInfo> result = couponFindService.getIssuedCoupons(couponSearch);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getContent()).hasSize(1),
                () -> {
                    CouponInfo couponInfo = result.getContent().get(0);
                    assertThat(couponInfo.getCoupon())
                            .isNotNull()
                            .isEqualTo(coupon);
                    assertThat(couponInfo.getIssuedCoupon())
                            .isNotNull()
                            .isEqualTo(issuedCoupon);
                },
                () -> assertThat(result.getTotalElements()).isEqualTo(1)
        );

        // verify
        verify(couponValidator).validateUserId(userId);
        verify(issuedCouponRepository).getIssuedCoupons(any(PageRequest.class), eq(userId));
        verify(couponRepository).getCoupon(couponId);
        verifyNoMoreInteractions(couponValidator, couponRepository, issuedCouponRepository);
    }

    @Test
    @DisplayName("유효하지 않은 userId - 쿠폰 조회 실패")
    void getCoupon_InvalidUserId_ThrowsException() {
        // given
        Long invalidUserId = -1L;
        CouponSearch couponSearch = CouponSearch.of(invalidUserId, 0, 10);

        // when
        willThrow(new IllegalArgumentException("유효하지 않은 유저 ID 입니다."))
                .given(couponValidator)
                .validateUserId(any(Long.class));

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            couponFindService.getIssuedCoupons(couponSearch);
        });

        // verify
        verify(couponValidator).validateUserId(invalidUserId);
        verifyNoInteractions(issuedCouponRepository, couponRepository);
    }
}
