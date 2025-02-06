package kr.hhplus.be.server.domain.coupon.usecase;

import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponScheduler {
    private final RedisTemplate<String, String> redisTemplate;
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    private static final String COUPON_REQUEST_KEY = "coupon-%s-requests";
    private static final String COUPON_ISSUED_KEY = "coupon-%s-issued";

    @Scheduled(fixedDelay = 1000) // 1초마다 실행
//    @DistributedLock(lockName = "coupon_issue_scheduler_lock", waitTime = 3000, leaseTime = 5000)
    public void issueCoupons() {

        Set<String> couponRequestKeys = redisTemplate.keys("coupon-*-requests");
        if (couponRequestKeys == null || couponRequestKeys.isEmpty()) {
            return;
        }

        List<Long> couponIds = couponRequestKeys.stream()
                .map(key -> key.split("-")[1])
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 1. 발급 가능한 쿠폰 조회
        LocalDate now = LocalDate.now();
        List<Coupon> availableCoupons = couponRepository.findAvailableCoupons(couponIds, now);
        for (Coupon coupon : availableCoupons) {
            try {
                issueCoupon(coupon);
            } catch (IllegalStateException e) {
                log.error("[쿠폰 재고 소진으로 발급 실패] couponId={}, error={}",
                        coupon.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("[쿠폰 발급 처리 중 오류 발생] couponId={}, error={}",
                        coupon.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void issueCoupon(Coupon coupon) {
        String couponRequestKey = String.format(COUPON_REQUEST_KEY, coupon.getId());
        String couponIssuedKey = String.format(COUPON_ISSUED_KEY, coupon.getId());

        // 1. 발급 가능 수량 계산
        long availableQuantity = coupon.getRemainingQuantity();
        if (availableQuantity <= 0) {
            throw new IllegalStateException(ErrorCode.COUPON_OUT_OF_STOCK.getMessage());
        }

        // 2. 발급 대상자 조회
        Set<ZSetOperations.TypedTuple<String>> requests =
                redisTemplate.opsForZSet().popMin(couponRequestKey, availableQuantity);
        if (requests == null || requests.isEmpty()) return;

        // 3. 쿠폰 발급 처리
        List<IssuedCoupon> issuedCoupons = new ArrayList<>();
        Set<String> issuedUsers = new HashSet<>();

        for (ZSetOperations.TypedTuple<String> request : requests) {
            String userId = request.getValue();
            if (userId == null) continue;

            IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                    .coupon(coupon)
                    .userId(Long.parseLong(userId))
                    .couponStatus(CouponStatusType.NEW)
                    .issuedAt(LocalDateTime.now())
                    .build();

            issuedCoupons.add(issuedCoupon);
            issuedUsers.add(userId);
        }

        if (issuedCoupons.isEmpty()) return;

        // 4. DB 저장
        coupon.decreaseRemainingQuantity(issuedCoupons.size());
        couponRepository.save(coupon);
        issuedCouponRepository.saveAll(issuedCoupons);

        // 5. Redis 발급 이력 저장
        redisTemplate.opsForSet().add(
                couponIssuedKey,
                issuedUsers.toArray(new String[0])
        );

        log.info("[쿠폰 발급 완료] couponId={}, issuedCount={}, remainingQuantity={}",
                coupon.getId(), issuedCoupons.size(), coupon.getRemainingQuantity());
    }
}
