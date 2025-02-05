package kr.hhplus.be.server.domain.coupon.usecase;

import kr.hhplus.be.server.common.aop.annotation.DistributedLock;
import kr.hhplus.be.server.common.aop.annotation.Monitored;
import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponInfo;
import kr.hhplus.be.server.domain.coupon.entity.Coupon;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
import kr.hhplus.be.server.domain.coupon.validation.CouponValidator;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponControlService {
    private final CouponFindService couponFindService;
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponValidator couponValidator;

    private final RedisTemplate<String, String> redisTemplate;
    private static final String COUPON_REQUEST_KEY = "coupon-%s-requests";
    private static final String COUPON_ISSUED_KEY = "coupon-%s-issued";

    /**
     * 쿠폰 사용하기
     */
    @Transactional
    public IssuedCoupon useIssuedCoupon(Long couponId) {
        if (couponId == null) {
            return null;
        }
        IssuedCoupon coupon = couponFindService.getIssuedCoupon(couponId);
        coupon.use();
        return coupon;
    }

    /**
     * 쿠폰 발급 요청하기
     */
    public CouponInfo requestCoupon(CouponCommand command) {
        log.info("[쿠폰 발급 요청 시작] userId={}, couponId={}", command.getUserId(), command.getCouponId());

        String couponRequestKey = String.format(COUPON_REQUEST_KEY, command.getCouponId());
        String couponIssuedKey = String.format(COUPON_ISSUED_KEY, command.getCouponId());

        try {
            // 1. 기본 유효성 검증
            couponValidator.validateUserId(command.getUserId());

            // 2. 중복 발급 확인
            Boolean isAlreadyIssued = redisTemplate.opsForSet()
                    .isMember(couponIssuedKey, command.getUserId().toString());

            if (Boolean.TRUE.equals(isAlreadyIssued)) {
                throw new IllegalStateException(ErrorCode.COUPON_ALREADY_ISSUED.getMessage());
            }

            // 3. 발급 요청 등록
            redisTemplate.opsForZSet()
                    .add(couponRequestKey,
                            command.getUserId().toString(), //member
                            System.currentTimeMillis()); //score

            log.info("[쿠폰 발급 요청 완료] userId={}, couponId={}",
                    command.getUserId(), command.getCouponId());

            return CouponInfo.builder()
                    .userId(command.getUserId())
//                    .couponId(command.getCouponId())
//                    .requestedAt(LocalDateTime.now())
//                    .status(CouponStatus.REQUESTED)
                    .build();

        } catch (Exception e) {
            log.error("[쿠폰 발급 요청 중 오류 발생]: couponId={}, userId={}, error={}",
                    command.getCouponId(), command.getUserId(), e.getMessage());
            throw new IllegalStateException("쿠폰 발급 요청 중 오류가 발생했습니다.");
        }
    }




//    /**
//     * 쿠폰 발급하기
//     */
//
//    @Monitored
//    @DistributedLock(lockName = "coupon_lock", waitTime = 5000, leaseTime = 10000)
//    public CouponInfo issueCoupon(CouponCommand command) {
//
//        log.info("[쿠폰 발급 시작] userId={}, couponId={}", command.getUserId(), command.getCouponId());
//
//        couponValidator.validateUserId(command.getUserId());
//        issuedCouponRepository.getUserIssuedCoupon(command.getCouponId(), command.getUserId())
//                .ifPresent(coupon -> {
//                    throw new IllegalStateException(ErrorCode.COUPON_ALREADY_ISSUED.getMessage());
//                });
//        log.info("[기존 발급 여부 확인 완료]");
//
//        try {
//            // 1. 쿠폰 수량 확인
//            Coupon coupon = couponRepository.getCoupon(command.getCouponId())
//                    .orElseThrow(() -> new NoSuchElementException(ErrorCode.INVALID_COUPON.getMessage() + " id=" + command.getCouponId()));
//            couponValidator.validateCouponQuantity(coupon);
//            log.info("[쿠폰 조회 완료] {}", coupon);
//
//            // 2. 쿠폰 잔량 감소
//            coupon.decreaseRemainingQuantity();
//            Coupon decreased = couponRepository.save(coupon);
//
//            // 3. 발급 내역 저장
//            IssuedCoupon issuedCoupon = IssuedCoupon.builder()
//                    .coupon(decreased)
//                    .userId(command.getUserId())
//                    .couponStatus(CouponStatusType.NEW)
//                    .issuedAt(LocalDateTime.now())
//                    .build();
//            IssuedCoupon issued = issuedCouponRepository.save(issuedCoupon);
//
//            log.info("[쿠폰 발급 완료] userId={}, couponId={}, issuedCouponId={}, beforeQuantity={}, remainingQuantity={}",
//                    command.getUserId(),
//                    command.getCouponId(),
//                    issued.getId(),
//                    coupon.getRemainingQuantity(),
//                    decreased.getRemainingQuantity());
//
//            return CouponInfo.builder()
//                    .coupon(coupon)
//                    .issuedCoupon(issuedCoupon)
//                    .build();
//        } catch (Exception e) {
//            log.error("[쿠폰 발급 처리 중 오류 발생]: couponId={}, userId={}, error={}",
//                    command.getCouponId(), command.getUserId(), e.getMessage());
//            throw new IllegalStateException("쿠폰 발급 처리 중 오류가 발생했습니다.");
//        }
//    }

    /**
     * 쿠폰 상태 복구하기
     */
    @Monitored
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revertCouponStatus(Long orderId, Long userId) {
        try {
            issuedCouponRepository.getOrderIssuedCoupon(orderId, userId)
                    .ifPresent(issuedCoupon -> {
                        issuedCoupon.revert();
                        issuedCouponRepository.save(issuedCoupon);
                        log.info("[쿠폰 상태 복구 완료] couponId={}", issuedCoupon.getId());
                    });
        }catch (Exception e) {
            log.error("[쿠폰 상태 복구 실패] orderId={}, userId={}", orderId, userId, e);
        }
    }
}