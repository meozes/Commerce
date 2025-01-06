package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponSearch;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.entity.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.usecase.CouponService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 수량 존재 - 발급 성공")
    void issueCoupon_Success() {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        CouponCommand command = new CouponCommand(couponId, userId);

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .remainingQuantity(5)
                .build();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .couponStatus(CouponStatusType.NEW)
                .issuedAt(LocalDateTime.now())
                .build();

        // when
        when(couponRepository.getCouponWithLock(couponId)).thenReturn(coupon);
        when(issuedCouponRepository.saveIssuedCoupon(any(IssuedCoupon.class))).thenReturn(issuedCoupon);

        CouponInfo result = couponService.issueCoupon(command);

        // then
        assertNotNull(result);
        assertEquals(couponId, result.getCoupon().getId());
        assertEquals(4, result.getCoupon().getRemainingQuantity()); // 발급 후 수량 감소 확인
        assertEquals(userId, result.getIssuedCoupon().getUserId());
        assertEquals(CouponStatusType.NEW, result.getIssuedCoupon().getCouponStatus());

        verify(couponRepository).getCouponWithLock(couponId);
        verify(couponRepository).saveCoupon(coupon);
        verify(issuedCouponRepository).saveIssuedCoupon(any(IssuedCoupon.class));
    }

    @Test
    @DisplayName("쿠폰 수량 부족 - 발급 실패")
    void issueCoupon_Fail() {
        // given
        Long couponId = 1L;
        Long userId = 1L;
        CouponCommand command = new CouponCommand(couponId, userId);

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .remainingQuantity(0) // 수량 0으로 설정
                .build();

        // when
        when(couponRepository.getCouponWithLock(couponId)).thenReturn(coupon);

        // then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> couponService.issueCoupon(command)
        );

        assertEquals("쿠폰이 모두 소진되었습니다." + couponId, exception.getMessage());
        verify(couponRepository).getCouponWithLock(couponId);
        verify(couponRepository, never()).saveCoupon(any(Coupon.class));
        verify(issuedCouponRepository, never()).saveIssuedCoupon(any(IssuedCoupon.class));
    }


    @Test
    @DisplayName("유효하지 않은 유저 ID - 쿠폰 조회 실패")
    void getCoupon_Fail() {
        // given
        Long invalidUserId = -1L;
        CouponSearch couponSearch = CouponSearch.of(invalidUserId, 0, 10);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> couponService.getCoupons(couponSearch));

        assertThat(exception.getMessage()).isEqualTo("유효하지 않은 유저 ID 입니다.");

    }

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
        when(issuedCouponRepository.getIssuedCoupons(any(PageRequest.class), eq(userId)))
                .thenReturn(issuedCouponPage);
        when(couponRepository.getCoupon(any(Long.class)))
                .thenReturn(coupon);

        // when
        Page<CouponInfo> result = couponService.getCoupons(couponSearch);

        // then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getContent()).hasSize(1),
                () -> {
                    CouponInfo couponInfo = result.getContent().get(0);
                    assertThat(couponInfo.getCoupon()).isNotNull();  // null 체크 추가
                    assertThat(couponInfo.getCoupon()).isEqualTo(coupon);
                },
                () -> assertThat(result.getContent().get(0).getIssuedCoupon()).isEqualTo(issuedCoupon),
                () -> assertThat(result.getTotalElements()).isEqualTo(1)
        );

        verify(issuedCouponRepository).getIssuedCoupons(any(PageRequest.class), eq(userId));
        verify(couponRepository).getCoupon(couponId);
    }
}
