package kr.hhplus.be.server.domain.coupon.repository;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface IssuedCouponRepository {
    IssuedCoupon save(IssuedCoupon issuedCoupon);

    Page<IssuedCoupon> getIssuedCoupons(PageRequest pageRequest, Long userId);

    IssuedCoupon saveIssuedCoupon(IssuedCoupon issuedCoupon);

    @Query("SELECT ic FROM IssuedCoupon ic JOIN FETCH ic.coupon c WHERE ic.id = :issueCouponId")
    Optional<IssuedCoupon> getIssuedCoupon(@Param("issueCouponId") Long issueCouponId);

    @Query("SELECT ic FROM IssuedCoupon ic JOIN FETCH ic.coupon c WHERE ic.couponId = :couponId")
    Optional<IssuedCoupon> getIssuedCouponByCoupon(Long couponId);

    @Query("SELECT ic FROM IssuedCoupon ic JOIN FETCH ic.coupon c WHERE ic.coupon.id = :couponId AND ic.userId = :userId")
    Optional<IssuedCoupon> getUserIssuedCoupon(@Param("couponId") Long couponId, @Param("userId") Long userId);
}
