package kr.hhplus.be.server.domain.coupon.validation;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponValidator {
    public void validateUserId(Long userId) {
        if (userId < 0) {
            throw new IllegalArgumentException("유효하지 않은 유저 ID 입니다.");
        }
    }

    public void validateCouponQuantity(Coupon coupon) {
        if (coupon.getRemainingQuantity() <= 0) {
            throw new IllegalArgumentException("쿠폰이 모두 소진되었습니다." + coupon.getId());
        }
    }
}
