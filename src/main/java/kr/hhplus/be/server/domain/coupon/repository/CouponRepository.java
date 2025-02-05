package kr.hhplus.be.server.domain.coupon.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Optional<Coupon> getCoupon(Long couponId);

    @Query("SELECT c FROM Coupon c " +
            "WHERE c.id IN :couponIds " +
            "AND c.remainingQuantity > 0 " +
            "AND c.dueDate >= :now")
    List<Coupon> findAvailableCoupons(
            @Param("couponIds") List<Long> couponIds,
            @Param("now") LocalDate now
    );

    Coupon save(Coupon coupon);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select c from Coupon c where c.id = :couponId")
    Optional<Coupon> getCouponWithLock(@Param("couponId") Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select c from Coupon c join IssuedCoupon ic on c = ic.coupon where ic.orderId = :orderId and ic.userId = :userId")
    Optional<Coupon> getCouponWithLock(@Param("orderId") Long orderId, @Param("userId") Long userId);
}


