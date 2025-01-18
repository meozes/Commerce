package kr.hhplus.be.server.domain.coupon.validation;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CouponValidator {
    public void validateUserId(Long userId) {
        if (userId < 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_USER_ID.getMessage());
        }
    }

    public void validateCouponQuantity(Coupon coupon) {
        if (coupon.getRemainingQuantity() <= 0) {
            throw new IllegalStateException(ErrorCode.COUPON_OUT_OF_STOCK.getMessage());
        }
    }
}
