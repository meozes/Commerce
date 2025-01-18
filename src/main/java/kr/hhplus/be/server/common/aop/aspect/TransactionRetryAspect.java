package kr.hhplus.be.server.common.aop.aspect;

import kr.hhplus.be.server.common.aop.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class TransactionRetryAspect {

    // 트랜잭션 실패시 재시도
    @Around("@annotation(kr.hhplus.be.server.common.aop.annotation.Retry)")
    public Object retry(ProceedingJoinPoint joinPoint) throws Throwable {
        Retry retry = ((MethodSignature) joinPoint.getSignature())
                .getMethod().getAnnotation(Retry.class);

        int maxAttempts = retry.maxAttempts();
        Throwable lastException = null;

        // 재시도 가능한 예외 목록 가져오기
        Class<? extends Throwable>[] retryableExceptions = retry.retryableExceptions();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (Exception e) {
                lastException = e;

                // 재시도 가능한 예외인지 확인
                boolean isRetryable = retryableExceptions.length == 0 || // 비어있으면 모든 예외 재시도
                        Arrays.stream(retryableExceptions)
                                .anyMatch(ex -> ex.isInstance(e));

                if (!isRetryable || attempt == maxAttempts) {
                    throw e;
                }

                log.warn("Attempt {} failed for method {}: {}",
                        attempt,
                        joinPoint.getSignature().getName(),
                        e.getMessage());

                try {
                    Thread.sleep(retry.delay());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        throw lastException;
    }
}
