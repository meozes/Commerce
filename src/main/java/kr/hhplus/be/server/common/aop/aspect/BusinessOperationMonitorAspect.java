package kr.hhplus.be.server.common.aop.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
@Slf4j
public class BusinessOperationMonitorAspect {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicInteger> activeCalls = new ConcurrentHashMap<>();

    public BusinessOperationMonitorAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(kr.hhplus.be.server.common.aop.annotation.Monitored)")
    public Object monitorBusinessOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String operationType = getOperationType(methodName);

        activeCalls.computeIfAbsent(operationType, k -> new AtomicInteger(0))
                .incrementAndGet();

        Timer.Sample timer = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            meterRegistry.counter("business.operation.success",
                    "operation", operationType,
                    "method", methodName).increment();

            return result;

        } catch (Exception e) {
            // 예외 메시지로 에러 코드 구분
            String errorMessage = e.getMessage();
            String errorType = categorizeError(errorMessage);

            meterRegistry.counter("business.operation.failure",
                    "operation", operationType,
                    "error_type", errorType,
                    "exception", e.getClass().getSimpleName()).increment();

            throw e;

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            timer.stop(Timer.builder("business.operation.duration")
                    .tags("operation", operationType)
                    .register(meterRegistry));

            activeCalls.get(operationType).decrementAndGet();

            meterRegistry.gauge("business.operation.active",
                    Tags.of("operation", operationType),
                    activeCalls.get(operationType));
        }
    }

    private String categorizeError(String errorMessage) {
        if (errorMessage == null) {
            return "UNKNOWN";
        }

        // 재고 관련 에러
        if (errorMessage.contains("상품") || errorMessage.contains("재고")) {
            return "STOCK_ERROR";
        }

        // 잔고 관련 에러
        if (errorMessage.contains("잔액") || errorMessage.contains("금액")) {
            return "INSUFFICIENT_BALANCE";
        }

        // 쿠폰 관련 에러
        if (errorMessage.contains("쿠폰")) {
            if (errorMessage.contains("소진")) {
                return "COUPON_OUT_OF_STOCK";
            }
            if (errorMessage.contains("만료")) {
                return "COUPON_EXPIRED";
            }
            return "COUPON_ERROR";
        }

        // 주문/결제 관련 에러
        if (errorMessage.contains("주문") || errorMessage.contains("결제")) {
            return "ORDER_PAYMENT_ERROR";
        }

        return "OTHER";
    }

    private String getOperationType(String methodName) {
        if (methodName.contains("charge") || methodName.contains("balance")) {
            return "BALANCE_OPERATION";
        } else if (methodName.contains("order") || methodName.contains("payment")) {
            return "ORDER_PAYMENT";
        } else if (methodName.contains("coupon")) {
            return "COUPON_OPERATION";
        } else if (methodName.contains("product")) {
            return "PRODUCT_OPERATION";
        }
        return "OTHER";
    }
}
