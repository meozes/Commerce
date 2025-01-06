package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.order.dto.OrderAmountInfo;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.service.OrderAmountCalculator;
import kr.hhplus.be.server.domain.order.usecase.*;
import kr.hhplus.be.server.domain.order.validation.OrderBalanceValidation;
import kr.hhplus.be.server.domain.order.validation.OrderCouponValidation;
import kr.hhplus.be.server.domain.order.validation.OrderProductValidation;
import kr.hhplus.be.server.domain.order.validation.OrderValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderService orderService;
    private final OrderBalanceValidation orderBalanceValidation;
    private final OrderCouponValidation orderCouponValidation;
    private final OrderValidation orderValidation;
    private final OrderProductValidation orderProductValidation;
    private final OrderAmountCalculator orderAmountCalculator;

    @Transactional
    public OrderInfo createOrder(OrderCommand command) {
        // 1. 주문 유효성 검증
        orderValidation.validateOrder(command);

        // 2. 잔고 확인
        orderBalanceValidation.handleBalance(command);

        // 3. 재고 확인
        orderProductValidation.handleProduct(command);

        // 4. 쿠폰 유효성 체크, 사용처리
        IssuedCoupon coupon = null;
        if (command.getCouponId() != null) {
            coupon = orderCouponValidation.handleCoupon(command);
        }

        // 5. 금액 계산
        OrderAmountInfo amountInfo = orderAmountCalculator.calculate(command, coupon);

        // 6. 최종 주문 생성
        return orderService.createOrder(command,
                amountInfo.getOriginalAmount(),
                amountInfo.getDiscountAmount(),
                amountInfo.getFinalAmount());
    }

}
