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

    @Query("SELECT ic FROM IssuedCoupon ic JOIN FETCH ic.coupon c WHERE ic.id = :issueCouponId")
    Optional<IssuedCoupon> getIssuedCoupon(@Param("issueCouponId") Long issueCouponId);

    @Query("SELECT ic FROM IssuedCoupon ic JOIN FETCH ic.coupon c WHERE ic.coupon.id = :couponId AND ic.userId = :userId")
    Optional<IssuedCoupon> getUserIssuedCoupon(@Param("couponId") Long couponId, @Param("userId") Long userId);

    @Query("SELECT ic FROM IssuedCoupon ic WHERE ic.orderId = :orderId AND ic.userId = :userId")
    Optional<IssuedCoupon> getOrderIssuedCoupon(@Param("orderId") Long orderId, @Param("userId") Long userId);
}
