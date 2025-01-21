package kr.hhplus.be.server.common.aop.aspect;

import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {
    private final DistributedLock distributedLock;

    @Around("@annotation(kr.hhplus.be.server.common.aop.annotation.DistributedLockOperation)")
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof CouponCommand)) {
            return joinPoint.proceed();
        }

        CouponCommand command = (CouponCommand) args[0];
        Long couponId = command.getCouponId();

        if (!distributedLock.acquireLock(couponId)) {
            throw new IllegalStateException("현재 다른 요청 처리중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            return joinPoint.proceed();
        } finally {
            distributedLock.releaseLock(couponId);
        }
    }
}
