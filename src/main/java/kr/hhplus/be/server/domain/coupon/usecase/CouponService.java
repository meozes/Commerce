package kr.hhplus.be.server.domain.coupon.usecase;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.coupon.dto.IssuedCouponInfo;
import kr.hhplus.be.server.domain.coupon.validation.CouponValidator;
import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponInfo;
import kr.hhplus.be.server.domain.coupon.dto.CouponSearch;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;


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
                    () -> new IllegalArgumentException("존재하지 않는 쿠폰입니다. id=" + issuedCoupon.getCoupon().getId())
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
        IssuedCoupon coupon = getIssuedCoupon(couponId);
        coupon.use();
        return coupon;
    }

    /**
     * 특정 발급 받은 쿠폰 조회하기
     */
    public IssuedCoupon getIssuedCoupon(Long issueCouponId) {
        return issuedCouponRepository.getIssuedCoupon(issueCouponId).orElseThrow(
                () -> new EntityNotFoundException("해당 쿠폰을 발급받은 내역이 존재하지 않습니다."));
    }

    /**
     * 쿠폰 발급하기
     */
    @Transactional
    public CouponInfo issueCoupon(CouponCommand command) {
        couponValidator.validateUserId(command.getUserId());
        Optional<IssuedCoupon> isAlreadyIssued = issuedCouponRepository.getIssuedCouponByCoupon(command.getCouponId());
        if (isAlreadyIssued.isPresent()){
            throw new IllegalArgumentException("이미 발급받은 쿠폰입니다.");
        }

        // 1. 쿠폰 수량 확인 (비관적 락)
        Coupon coupon = couponRepository.getCouponWithLock(command.getCouponId());
        if (coupon == null) {
            throw new EntityNotFoundException("존재하지 않는 쿠폰입니다. id=" + command.getCouponId());
        }
        couponValidator.validateCouponQuantity(coupon);

        // 2. 쿠폰 잔량 감소
        coupon.decreaseRemainingQuantity();
        couponRepository.saveCoupon(coupon);

        // 3. 발급 내역 저장
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
               .coupon(coupon)
               .userId(command.getUserId())
                .couponStatus(CouponStatusType.NEW)
                .issuedAt(LocalDateTime.now())
               .build();
        issuedCouponRepository.saveIssuedCoupon(issuedCoupon);

        return CouponInfo.builder()
               .coupon(coupon)
               .issuedCoupon(issuedCoupon)
               .build();

    }

}
