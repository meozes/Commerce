package kr.hhplus.be.server.infra.coupon;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssuedCouponRepositoryImpl extends JpaRepository<IssuedCoupon, Long>, IssuedCouponRepository {
    Page<IssuedCoupon> findAllByUserId(PageRequest pageRequest, Long userId);

    @Override
    default Page<IssuedCoupon> getIssuedCoupons(PageRequest pageRequest, Long userId) {
        return findAllByUserId(pageRequest, userId);
    }

    @Override
    default Optional<IssuedCoupon> getIssuedCoupon(Long issueCouponId){
        return findById(issueCouponId);
    }

    Optional<IssuedCoupon> findByCouponId(Long couponId);

    List<IssuedCoupon> findAllByCouponId(Long couponId);

}
