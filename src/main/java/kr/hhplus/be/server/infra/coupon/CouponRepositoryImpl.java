package kr.hhplus.be.server.infra.coupon;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepositoryImpl extends JpaRepository<Coupon, Long>, CouponRepository {
    @Override
    default Coupon getCoupon(Long couponId) {
        return findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다. id=" + couponId));
    }

    @Override
    default void saveCoupon(Coupon coupon) {
        saveAndFlush(coupon);
    }
}
