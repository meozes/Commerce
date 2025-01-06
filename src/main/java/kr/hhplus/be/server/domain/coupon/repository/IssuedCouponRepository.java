package kr.hhplus.be.server.domain.coupon.repository;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;



public interface IssuedCouponRepository {
    Page<IssuedCoupon> getIssuedCoupons(PageRequest pageRequest, Long userId);
    IssuedCoupon saveIssuedCoupon(IssuedCoupon issuedCoupon);
}
