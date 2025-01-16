package kr.hhplus.be.server.domain.coupon.usecase;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.aop.annotation.Monitored;
import kr.hhplus.be.server.domain.coupon.validation.CouponValidator;
import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponInfo;
import kr.hhplus.be.server.domain.coupon.dto.CouponSearch;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;


@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponValidator couponValidator;

    /**
     * 유저가 발급받은 쿠폰 내역 조회
     */
    public Page<CouponInfo> getIssuedCoupons(CouponSearch couponSearch) {
        couponValidator.validateUserId(couponSearch.getUserId());
        Page<IssuedCoupon> issuedCoupons = issuedCouponRepository.getIssuedCoupons(couponSearch.toPageRequest(), couponSearch.getUserId());

        return issuedCoupons.map(issuedCoupon -> {
            Coupon coupon = couponRepository.getCoupon(issuedCoupon.getCoupon().getId()).orElseThrow(
                    () -> new NoSuchElementException(ErrorCode.INVALID_COUPON.getMessage() + " id=" + issuedCoupon.getCoupon().getId())
            );
            return CouponInfo.builder()
                    .coupon(coupon)
                    .issuedCoupon(issuedCoupon)
                    .build();
        });
    }

    /**
     * 쿠폰 사용하기
     */
    @Transactional
    public IssuedCoupon useIssuedCoupon(Long couponId) {
        if (couponId == null){
            return null;
        }
        IssuedCoupon coupon = getIssuedCoupon(couponId);
        coupon.use();
        return coupon;
    }

    /**
     * 특정 발급 받은 쿠폰 조회하기
     */
    public IssuedCoupon getIssuedCoupon(Long issueCouponId) {
        return issuedCouponRepository.getIssuedCoupon(issueCouponId).orElseThrow(
                () -> new NoSuchElementException(ErrorCode.COUPON_NOT_FOUND.getMessage()));
    }

    /**
     * 쿠폰 발급하기
     */
    @Monitored
    @Transactional
    public CouponInfo issueCoupon(CouponCommand command) {

        log.info("[쿠폰 발급 시작] userId={}, couponId={}", command.getUserId(), command.getCouponId());

        couponValidator.validateUserId(command.getUserId());
        issuedCouponRepository.getUserIssuedCoupon(command.getCouponId(), command.getUserId())
                .ifPresent(coupon -> {
                    throw new IllegalStateException(ErrorCode.COUPON_ALREADY_ISSUED.getMessage());
                });

        // 1. 쿠폰 수량 확인 (비관적 락)
        Coupon coupon = couponRepository.getCouponWithLock(command.getCouponId())
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.INVALID_COUPON.getMessage()+ " id=" + command.getCouponId()));
        couponValidator.validateCouponQuantity(coupon);

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

    }

    /**
     * 쿠폰 수량 복구하기
     */
    @Monitored
    @Transactional
    public void revertRemainingQuantity(Long orderId, Long userId) {

        couponRepository.getCouponWithLock(orderId, userId)
                .ifPresent(
                        coupon -> {
                    coupon.revertRemainingQuantity();
                    couponRepository.save(coupon);

                    IssuedCoupon issuedCoupon = issuedCouponRepository.getOrderIssuedCoupon(orderId, userId)
                            .orElseThrow(() -> new NoSuchElementException(ErrorCode.COUPON_NOT_FOUND.getMessage()));
                    issuedCoupon.revert();
                    issuedCouponRepository.save(issuedCoupon);

                    log.info("[쿠폰 복구 완료] couponId={}, remainingCouponQuantity={}" , coupon.getId(), coupon.getRemainingQuantity());
                });

    }
}
