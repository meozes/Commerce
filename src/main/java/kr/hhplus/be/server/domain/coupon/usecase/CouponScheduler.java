package kr.hhplus.be.server.domain.coupon.usecase;


import kr.hhplus.be.server.common.aop.annotation.DistributedLock;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    public void issueCoupons() {

        log.info("[쿠폰 발급 스케쥴러 실행]");

        List<Coupon> couponList = couponRepository.getCoupons();
        Set<String> keys = redisTemplate.keys("coupon-*-requests");
        if (keys == null || keys.isEmpty()) {
            log.info("[발급 요청된 쿠폰이 없습니다]");
            return;
        }

        Set<String> couponRequestKeys = new HashSet<>();
        for (String key : keys) {
            Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet()
                    .rangeWithScores(key, 0, -1);

            Set<String> members = typedTuples.stream()
                    .map(ZSetOperations.TypedTuple::getValue)
                    .collect(Collectors.toSet());
            couponRequestKeys.addAll(members);
        }

        List<Long> couponIds = keys.stream()
                .map(key -> key.split("-")[1])
                .map(Long::parseLong)
                .collect(Collectors.toList());


        // 1. 발급 가능한 쿠폰 조회
        LocalDate now = LocalDate.now();
        List<Coupon> availableCoupons = couponRepository.findAvailableCoupons(couponIds, now);
        log.info("[발급 가능한 쿠폰 조회 완료] availableCoupons = {}", availableCoupons.size());

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
        log.info("[쿠폰 발급 수량 계산 완료] couponId = {}, availableQ = {}", coupon.getId(), availableQuantity);

        // 2. 발급 대상자 조회
        BoundZSetOperations<String, String> boundZSetOps = redisTemplate.boundZSetOps(couponRequestKey);
        if(boundZSetOps.size() == 0) return;

        Set<ZSetOperations.TypedTuple<String>> requests = new HashSet<>();
        for (int i = 0; i < availableQuantity; i++) {
            try {
                Set<ZSetOperations.TypedTuple<String>> popped = boundZSetOps.popMin(1);
                popped.forEach(tuple -> {
                    requests.add(tuple);
                });
            } catch (Exception e) {
                log.error("발급 대상 조회 중 오류 발생: {}", e.getMessage());
                throw e;
            }
            log.info("[쿠폰 발급 대상 조회 완료]");
        }

        // 3. 쿠폰 발급 처리
        List<IssuedCoupon> issuedCoupons = new ArrayList<>();
        Set<String> issuedUsers = new HashSet<>();

        for (ZSetOperations.TypedTuple<String> tuple : requests) {
            String userId = tuple.getValue();
            if (userId == null) continue;

            IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                    .coupon(coupon)
                    .userId(Long.parseLong(userId))
                    .couponStatus(CouponStatusType.NEW)
                    .issuedAt(LocalDateTime.now())
                    .build();

            issuedCoupons.add(issuedCoupon);
            issuedUsers.add(userId);
            log.info("[쿠폰 발급 처리 완료] couponId = {}, userId = {}", coupon.getId(), userId);
        }
        if (issuedCoupons.isEmpty()) return;

        // 4. DB 저장
        coupon.decreaseRemainingQuantity(issuedCoupons.size());
        couponRepository.save(coupon);
        issuedCoupons.forEach(issuedCouponRepository::save);
        log.info("[쿠폰 발급 DB 저장 완료] couponId={}", coupon.getId());

        // 5. Redis 발급 이력 저장
        redisTemplate.opsForSet().add(
                couponIssuedKey,
                issuedUsers.toArray(new String[0])
        );

        log.info("[쿠폰 발급 완료] couponId={}, issuedCount={}, remainingQuantity={}",
                coupon.getId(), issuedCoupons.size(), coupon.getRemainingQuantity());
    }

}
