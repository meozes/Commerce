package kr.hhplus.be.server.common.aop.aspect;

import kr.hhplus.be.server.common.aop.annotation.DistributedLock;
import kr.hhplus.be.server.common.lock.RedissonDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class DistributedLockAspect {
    private final RedissonDistributedLock distributedLock;

    @Around("@annotation(kr.hhplus.be.server.common.aop.annotation.DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        DistributedLock distributedLockAnnotation = signature.getMethod().getAnnotation(DistributedLock.class);

        Long key = distributedLockAnnotation.key();
        long waitTime = distributedLockAnnotation.waitTime();
        long leaseTime = distributedLockAnnotation.leaseTime();

        boolean isLocked = false;

        try {
            isLocked = distributedLock.acquireLock(key, waitTime, leaseTime);
            if (!isLocked) {
                throw new IllegalStateException("락 획득 실패");
            }
            return joinPoint.proceed(); // 실제 메서드 실행
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락 획득 중 인터럽트 발생", e);
        } finally {
            if (isLocked) {
                distributedLock.releaseLock(key);
            }
        }
    }

}
