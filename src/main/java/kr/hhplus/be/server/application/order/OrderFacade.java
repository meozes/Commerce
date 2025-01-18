package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.balance.dto.BalanceQuery;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.usecase.CouponControlService;
import kr.hhplus.be.server.domain.order.dto.OrderAmountInfo;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.service.OrderAmountCalculator;
import kr.hhplus.be.server.domain.order.usecase.*;
import kr.hhplus.be.server.domain.order.validation.OrderValidator;
import kr.hhplus.be.server.domain.product.usecase.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderCreateService orderCreateService;
    private final BalanceService balanceService;
    private final CouponControlService couponControlService;
    private final StockService stockService;
    private final OrderValidator orderValidator;
    private final OrderAmountCalculator orderAmountCalculator;


    @Transactional
    public OrderInfo createOrder(OrderCommand command) {

        log.info("[주문 프로세스 시작] userId={}, orderItems={}, hasCoupon={}",
                command.getUserId(),
                command.getOrderItems().stream()
                        .map(item -> String.format("상품ID:%d, 수량:%d", item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList()),
                command.getCouponId() != null);

        // 1. 주문 유효성 검증
        orderValidator.validateOrder(command);
        log.info("[주문 검증 완료] userId={}", command.getUserId());

        // 2. 계좌 여부 확인
        balanceService.getBalance(BalanceQuery.of(command.getUserId()));
        log.info("[계좌 여부 확인 완료] userId={}", command.getUserId());

        // 3. 재고 확인, 차감
        stockService.validateAndDeductStock(command.getOrderItems());
        log.info("[재고 차감 완료] orderItems_size={}", command.getOrderItems().size());

        // 4. 쿠폰 사용처리
        IssuedCoupon coupon = command.getCouponId() != null ? couponControlService.useIssuedCoupon(command.getCouponId()) : null;
        log.info("[쿠폰 사용 처리 완료] couponId={}", command.getCouponId());

        // 5. 금액 계산
        OrderAmountInfo amountInfo = orderAmountCalculator.calculate(command.getOrderItems(), coupon);
        log.info("[최종 금액 계산 완료] original={} discounted={} final={}", amountInfo.getOriginalAmount(), amountInfo.getDiscountAmount(), amountInfo.getFinalAmount());

        // 6. 최종 주문 생성
        OrderInfo orderInfo = orderCreateService.createOrder(
                command,
                amountInfo.getOriginalAmount(),
                amountInfo.getDiscountAmount(),
                amountInfo.getFinalAmount(),
                coupon
        );

        log.info("[주문 프로세스 완료] orderId={}, userId={}, originalAmount={}, discountAmount={}, finalAmount={}",
                orderInfo.getOrder().getId(),
                command.getUserId(),
                amountInfo.getOriginalAmount(),
                amountInfo.getDiscountAmount(),
                amountInfo.getFinalAmount());

        return orderInfo;
    }

}
