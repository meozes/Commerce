package kr.hhplus.be.server.domain.order.validation;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.usecase.CouponService;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderCouponValidation {
    private final CouponService couponService;

    public IssuedCoupon handleCoupon(OrderCommand command) {
        IssuedCoupon coupon = couponService.getIssuedCoupon(command.getCouponId());
        coupon.use();
        couponService.saveIssuedCoupon(coupon);
        return coupon;
    }
}
