package kr.hhplus.be.server.domain.coupon.usecase;

import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponInfo;
import kr.hhplus.be.server.domain.coupon.dto.CouponSearch;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    public Page<CouponInfo> getCoupons(CouponSearch couponSearch) {

        if (couponSearch.getUserId() < 0) {
            throw new IllegalArgumentException("유효하지 않은 유저 ID 입니다.");
        }

        Page<IssuedCoupon> issuedCoupons = issuedCouponRepository.getIssuedCoupons(couponSearch.toPageRequest(), couponSearch.getUserId());

        return issuedCoupons.map(issuedCoupon -> {
            Coupon coupon = couponRepository.getCoupon(issuedCoupon.getCoupon().getId());

            return CouponInfo.builder()
                    .coupon(coupon)
                    .issuedCoupon(issuedCoupon)
                    .build();
        });

    }

    public CouponInfo issueCoupon(CouponCommand command) {
        if (command.getUserId() < 0) {
            throw new IllegalArgumentException("유효하지 않은 유저 ID 입니다.");
        }

        //coupon 수량 확인
        Coupon coupon = couponRepository.getCouponWithLock(command.getCouponId());
        if (coupon.getRemainingQuantity() <= 0) {
            throw new IllegalArgumentException("쿠폰이 모두 소진되었습니다." + coupon.getId());
        }

        //coupon update
        coupon.issue();
        couponRepository.saveCoupon(coupon);

        //issuedCoupon insert
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
               .coupon(coupon)
               .userId(command.getUserId())
                .couponStatus(CouponStatusType.NEW)
                .issuedAt(LocalDateTime.now())
               .build();

        issuedCouponRepository.saveIssuedCoupon(issuedCoupon);

        return CouponInfo.builder()
               .coupon(coupon)
               .issuedCoupon(issuedCoupon)
               .build();

    }
}
