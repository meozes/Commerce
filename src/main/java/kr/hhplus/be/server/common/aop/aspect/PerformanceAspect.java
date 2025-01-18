package kr.hhplus.be.server.common.aop.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    // 시간이 오래 걸리는 메서드 모니터링
    @Around("@annotation(kr.hhplus.be.server.common.aop.annotation.Monitoring)")
    public Object checkPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();

        if ((endTime - startTime) > 1000) { // 1초 이상 걸리는 경우
            log.warn("Slow Method Detected: {}.{} - {}ms",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    endTime - startTime
            );
        }
        return result;
    }
}
