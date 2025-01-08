package kr.hhplus.be.server.domain.order.validation;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.usecase.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCouponValidation {
    private final CouponService couponService;

    @Transactional
    public IssuedCoupon handleCoupon(Long couponId) {
        IssuedCoupon coupon = couponService.getIssuedCoupon(couponId);
        coupon.use();
        return coupon;
    }
}
