package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.balance.exception.NotEnoughBalanceException;
import kr.hhplus.be.server.domain.coupon.usecase.CouponControlService;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.usecase.OrderControlService;
import kr.hhplus.be.server.domain.order.usecase.OrderFindService;
import kr.hhplus.be.server.domain.order.validation.OrderValidator;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import kr.hhplus.be.server.interfaces.external.OrderEventSender;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import kr.hhplus.be.server.domain.payment.dto.PaymentInfo;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
import kr.hhplus.be.server.domain.payment.usecase.PaymentService;
import kr.hhplus.be.server.domain.product.usecase.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final OrderControlService orderControlService;
    private final OrderFindService orderFindService;
    private final PaymentService paymentService;
    private final BalanceService balanceService;
    private final StockService stockService;
    private final CouponControlService couponControlService;
    private final OrderValidator orderValidator;
    private final OrderEventSender orderEventSender;

    @Transactional
    public PaymentInfo createPayment(PaymentCommand command) {

        log.info("[결제 프로세스 시작] orderId={}, userId={}, amount={}",
                command.getOrderId(),
                command.getUserId(),
                command.getAmount());

        // 1. 주문 조회
        Order order = orderValidator.validateOrder(orderFindService.getOrder(command.getOrderId()), command);
        List<OrderItem> items = orderFindService.getOrderItems(order.getId());
        log.info("[주문 조회 완료] orderId={}, orderStatus={}, orderItems={}",
                order.getId(),
                order.getOrderStatus(),
                items.stream()
                        .map(item -> String.format("상품ID:%d, 수량:%d", item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList()));

        // 2. 결제 조회
        paymentService.getPayment(command)
                .ifPresent(payment -> {
                    throw new IllegalStateException(ErrorCode.PAYMENT_ALREADY_COMPLETED.getMessage());
                });

        try {
            // 3. 잔고 조회, 차감
            balanceService.deductBalance(command.getUserId(), command.getAmount());
        } catch (NotEnoughBalanceException e) {
            log.error("[결제 실패 - 잔액 부족] orderId={}, userId={}, requestAmount={}",
                    command.getOrderId(),
                    command.getUserId(),
                    command.getAmount());

            // 3-1. 재고 복구
            stockService.restoreStock(items, order.getId(), command.getUserId());
            // 3-2. 쿠폰 상태 복구
            couponControlService.revertCouponStatus(order.getId(), command.getUserId());
            // 3-3. 주문 취소
            orderControlService.cancelOrder(order);

            throw new IllegalStateException(ErrorCode.INSUFFICIENT_BALANCE.getMessage());
        }

        // 4. 주문 상태 업데이트
        Order updatedOrder = orderControlService.completeOrder(order);

        // 5. 결제 완료
        Payment completedPayment = paymentService.completePayment(command, order);
        log.info("[결제 완료] orderId={}, userId={}, amount={}, paymentId={}",
                order.getId(),
                command.getUserId(),
                command.getAmount(),
                completedPayment.getId());

        // 6. 외부 데이터 플랫폼으로 주문 정보 전송
        try {
            orderEventSender.send(updatedOrder);
        } catch (RuntimeException | InterruptedException e) {
            log.error(ErrorCode.ORDER_SYNC_FAILED.getMessage(), e);
        }

        return PaymentInfo.from(completedPayment);
    }

}

