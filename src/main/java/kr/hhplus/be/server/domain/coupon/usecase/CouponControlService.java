package kr.hhplus.be.server.domain.coupon.usecase;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.aop.annotation.DistributedLock;
import kr.hhplus.be.server.common.aop.annotation.Monitored;
import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponInfo;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.validation.CouponValidator;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponControlService {
    private final CouponFindService couponFindService;
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponValidator couponValidator;

    /**
     * 쿠폰 사용하기
     */
    @Transactional
    public IssuedCoupon useIssuedCoupon(Long couponId) {
        if (couponId == null) {
            return null;
        }
        IssuedCoupon coupon = couponFindService.getIssuedCoupon(couponId);
        coupon.use();
        return coupon;
    }

    /**
     * 쿠폰 발급하기
     */

    @Monitored
    @DistributedLock
    public CouponInfo issueCoupon(CouponCommand command) {

        log.info("[쿠폰 발급 시작] userId={}, couponId={}", command.getUserId(), command.getCouponId());

        couponValidator.validateUserId(command.getUserId());
        issuedCouponRepository.getUserIssuedCoupon(command.getCouponId(), command.getUserId())
                .ifPresent(coupon -> {
                    throw new IllegalStateException(ErrorCode.COUPON_ALREADY_ISSUED.getMessage());
                });
        log.info("[기존 발급 여부 확인 완료]");

        try {
            // 1. 쿠폰 수량 확인
            Coupon coupon = couponRepository.getCoupon(command.getCouponId())
                    .orElseThrow(() -> new NoSuchElementException(ErrorCode.INVALID_COUPON.getMessage() + " id=" + command.getCouponId()));
            couponValidator.validateCouponQuantity(coupon);
            log.info("[쿠폰 조회 완료] {}", coupon);

            // 2. 쿠폰 잔량 감소
            coupon.decreaseRemainingQuantity();
            Coupon decreased = couponRepository.save(coupon);

            // 3. 발급 내역 저장
            IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                    .coupon(decreased)
                    .userId(command.getUserId())
                    .couponStatus(CouponStatusType.NEW)
                    .issuedAt(LocalDateTime.now())
                    .build();
            IssuedCoupon issued = issuedCouponRepository.save(issuedCoupon);

            log.info("[쿠폰 발급 완료] userId={}, couponId={}, issuedCouponId={}, beforeQuantity={}, remainingQuantity={}",
                    command.getUserId(),
                    command.getCouponId(),
                    issued.getId(),
                    coupon.getRemainingQuantity(),
                    decreased.getRemainingQuantity());

            return CouponInfo.builder()
                    .coupon(coupon)
                    .issuedCoupon(issuedCoupon)
                    .build();
        } catch (Exception e) {
            log.error("[쿠폰 발급 처리 중 오류 발생]: couponId={}, userId={}, error={}",
                    command.getCouponId(), command.getUserId(), e.getMessage());
            throw new IllegalStateException("쿠폰 발급 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 쿠폰 상태 복구하기
     */
    @Monitored
    @Transactional
    public void revertCouponStatus(Long orderId, Long userId) {

        issuedCouponRepository.getOrderIssuedCoupon(orderId, userId)
                .ifPresent(issuedCoupon -> {
                    issuedCoupon.revert();
                    issuedCouponRepository.save(issuedCoupon);
                    log.info("[쿠폰 상태 복구 완료] couponId={}", issuedCoupon.getId());
                });
    }
}