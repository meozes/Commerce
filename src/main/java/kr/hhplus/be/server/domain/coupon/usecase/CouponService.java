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


@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponValidator couponValidator;

    public Page<CouponInfo> getCoupons(CouponSearch couponSearch) {
        couponValidator.validateUserId(couponSearch.getUserId());
        Page<IssuedCoupon> issuedCoupons = issuedCouponRepository.getIssuedCoupons(couponSearch.toPageRequest(), couponSearch.getUserId());

        return issuedCoupons.map(issuedCoupon -> {
            Coupon coupon = couponRepository.getCoupon(issuedCoupon.getCoupon().getId());
            return CouponInfo.builder()
                    .coupon(coupon)
                    .issuedCoupon(issuedCoupon)
                    .build();
        });
    }

    public IssuedCoupon getIssuedCoupon(Long issueCouponId) {
        return issuedCouponRepository.getIssuedCoupon(issueCouponId).orElseThrow(
                () -> new EntityNotFoundException("해당 쿠폰이 존재하지 않습니다."));
    }

    @Transactional
    public CouponInfo issueCoupon(CouponCommand command) {
        couponValidator.validateUserId(command.getUserId());

        //coupon 수량 확인
        Coupon coupon = couponRepository.getCouponWithLock(command.getCouponId());
        couponValidator.validateCouponQuantity(coupon);

        //coupon 발급
        coupon.issue();
        couponRepository.saveCoupon(coupon);

        //발급 내역 저장
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

    @Transactional
    public void saveIssuedCoupon(IssuedCoupon coupon) {
        issuedCouponRepository.saveIssuedCoupon(coupon);
    }
}
