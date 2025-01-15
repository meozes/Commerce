package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.balance.dto.BalanceQuery;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.coupon.usecase.CouponService;
import kr.hhplus.be.server.domain.order.dto.OrderAmountInfo;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.service.OrderAmountCalculator;
import kr.hhplus.be.server.domain.order.usecase.*;
import kr.hhplus.be.server.domain.order.validation.OrderValidator;
import kr.hhplus.be.server.domain.product.usecase.ProductService;
import kr.hhplus.be.server.domain.product.usecase.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderCreateService orderCreateService;
    private final BalanceService balanceService;
    private final CouponService couponService;
    private final ProductService productService;
    private final StockService stockService;
    private final OrderValidator orderValidator;
    private final OrderAmountCalculator orderAmountCalculator;


    @Transactional
    public OrderInfo createOrder(OrderCommand command) {

        // 1. 주문 유효성 검증
        orderValidator.validateOrder(command);

        // 2. 계좌 여부 확인
        balanceService.getBalance(BalanceQuery.of(command.getUserId()));

        // 3. 재고 확인, 차감
        productService.getOrderProduct(command.getOrderItems());
        stockService.deductStock(command.getOrderItems());

        // 4. 쿠폰 사용처리
        IssuedCoupon coupon = command.getCouponId() != null ? couponService.useIssuedCoupon(command.getCouponId()) : null;

        // 5. 금액 계산
        OrderAmountInfo amountInfo = orderAmountCalculator.calculate(command.getOrderItems(), coupon);

        // 6. 최종 주문 생성
        return orderCreateService.createOrder(
                command,
                amountInfo.getOriginalAmount(),
                amountInfo.getDiscountAmount(),
                amountInfo.getFinalAmount(),
                coupon
        );
    }

}
