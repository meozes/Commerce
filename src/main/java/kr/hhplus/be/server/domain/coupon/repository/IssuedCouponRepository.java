package kr.hhplus.be.server.domain.coupon.repository;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface IssuedCouponRepository {
    Page<IssuedCoupon> getIssuedCoupons(PageRequest pageRequest, Long userId);
    IssuedCoupon saveIssuedCoupon(IssuedCoupon issuedCoupon);

    @Query("SELECT ic FROM IssuedCoupon ic JOIN FETCH ic.coupon c WHERE ic.id = :issueCouponId")
    IssuedCoupon getIssuedCoupon(@Param("issueCouponId") Long issueCouponId);
}
