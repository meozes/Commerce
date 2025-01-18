package kr.hhplus.be.server.domain.coupon.usecase;

import kr.hhplus.be.server.domain.coupon.dto.CouponInfo;
import kr.hhplus.be.server.domain.coupon.dto.CouponSearch;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.validation.CouponValidator;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponFindService {
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
     * 특정 발급 받은 쿠폰 조회하기
     */
    public IssuedCoupon getIssuedCoupon(Long issueCouponId) {
        return issuedCouponRepository.getIssuedCoupon(issueCouponId).orElseThrow(
                () -> new NoSuchElementException(ErrorCode.COUPON_NOT_FOUND.getMessage()));
    }
}
