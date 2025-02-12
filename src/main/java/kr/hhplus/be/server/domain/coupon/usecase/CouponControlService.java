package kr.hhplus.be.server.domain.coupon.usecase;

import kr.hhplus.be.server.common.aop.annotation.Monitored;
import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.coupon.dto.CouponIssueRequestInfo;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.repository.CouponRepository;
import kr.hhplus.be.server.domain.coupon.repository.IssuedCouponRepository;
import kr.hhplus.be.server.domain.coupon.validation.CouponValidator;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

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
    public CouponIssueRequestInfo requestCoupon(CouponCommand command) {
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
            redisTemplate.expire(couponRequestKey, Duration.ofHours(1)); // ttl
            
            log.info("[쿠폰 발급 요청 완료] userId={}, couponId={}",
                    command.getUserId(), command.getCouponId());

            return CouponIssueRequestInfo.builder()
                    .userId(command.getUserId())
                    .couponId(command.getCouponId())
                    .requestedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("[쿠폰 발급 요청 중 오류 발생]: couponId={}, userId={}, error={}",
                    command.getCouponId(), command.getUserId(), e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }


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