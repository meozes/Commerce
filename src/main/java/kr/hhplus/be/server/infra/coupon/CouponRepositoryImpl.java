package kr.hhplus.be.server.infra.coupon;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepositoryImpl extends JpaRepository<Coupon, Long>, CouponRepository {

    @Override
    default Optional<Coupon> getCoupon(Long couponId) {return findById(couponId);}

}
