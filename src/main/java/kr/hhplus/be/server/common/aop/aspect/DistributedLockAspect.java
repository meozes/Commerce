package kr.hhplus.be.server.common.aop.aspect;

import kr.hhplus.be.server.common.lock.DistributedLock;
import kr.hhplus.be.server.domain.coupon.dto.CouponCommand;
import kr.hhplus.be.server.domain.order.dto.OrderItemCommand;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class DistributedLockAspect {
    private final DistributedLock distributedLock;

    @Around("@annotation(kr.hhplus.be.server.common.aop.annotation.DistributedLock)")
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (args.length > 0 && args[0] instanceof CouponCommand) {
            return handleCouponLock(joinPoint, (CouponCommand) args[0]);
        }

        if (args.length > 0 && args[0] instanceof List<?>) {
            List<?> items = (List<?>) args[0];
            if (!items.isEmpty() && items.get(0) instanceof OrderItemCommand) {
                return handleDeductStock(joinPoint, (List<OrderItemCommand>) items);
            }
            else if(!items.isEmpty() && items.get(0) instanceof OrderItem) {
                return handleRestoreStock(joinPoint, (List<OrderItem>) items);
            }

        }

        return joinPoint.proceed();
    }

    private Object handleCouponLock(ProceedingJoinPoint joinPoint, CouponCommand command) throws Throwable {
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

    private Object handleDeductStock(ProceedingJoinPoint joinPoint, List<OrderItemCommand> orderItems) throws Throwable {
        // 상품 ID 기준으로 정렬하여 교착 상태 방지
        List<Long> productIds = orderItems.stream()
                .map(OrderItemCommand::getProductId)
                .sorted()
                .distinct()
                .toList();

        // 모든 상품에 대해 락 획득 시도
        for (Long productId : productIds) {
            if (!distributedLock.acquireLock(productId)) {
                // 이미 획득한 락들 해제
                productIds.stream()
                        .takeWhile(id -> !id.equals(productId))
                        .forEach(distributedLock::releaseLock);

                throw new IllegalStateException("현재 다른 요청 처리중입니다. 잠시 후 다시 시도해주세요.");
            }
        }

        try {
            return joinPoint.proceed();
        } finally {
            // 모든 락 해제
            productIds.forEach(distributedLock::releaseLock);
        }
    }

    private Object handleRestoreStock(ProceedingJoinPoint joinPoint, List<OrderItem> orderItems) throws Throwable {
        // 상품 ID 기준으로 정렬하여 교착 상태 방지
        List<Long> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .sorted()
                .distinct()
                .toList();

        // 모든 상품에 대해 락 획득 시도
        for (Long productId : productIds) {
            if (!distributedLock.acquireLock(productId)) {
                // 이미 획득한 락들 해제
                productIds.stream()
                        .takeWhile(id -> !id.equals(productId))
                        .forEach(distributedLock::releaseLock);

                throw new IllegalStateException("현재 다른 요청 처리중입니다. 잠시 후 다시 시도해주세요.");
            }
        }

        try {
            return joinPoint.proceed();
        } finally {
            // 모든 락 해제
            productIds.forEach(distributedLock::releaseLock);
        }
    }


}
