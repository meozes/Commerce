package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.balance.exception.NotEnoughBalanceException;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final OrderControlService orderControlService;
    private final OrderFindService orderFindService;
    private final PaymentService paymentService;
    private final BalanceService balanceService;
    private final StockService stockService;
    private final OrderValidator orderValidator;
    private final OrderEventSender orderEventSender;

    @Transactional
    public PaymentInfo createPayment(PaymentCommand command) {
        // 1. 주문 조회
        Order order = orderValidator.validateOrder(orderFindService.getOrder(command.getOrderId()), command);
        List<OrderItem> items = orderFindService.getOrderItems(order.getId());

        try {
            // 2. 잔고 조회, 차감
            balanceService.deductBalance(command.getUserId(), command.getAmount());
        } catch (NotEnoughBalanceException e) {
            // 3. 실패 시 재고 복구, 결제 실패 처리
            stockService.restoreStock(items);
            orderControlService.cancelOrder(order);
            completeFailedPayment(command);
            throw new IllegalStateException(ErrorCode.INSUFFICIENT_BALANCE.getMessage());
        }

        // 4. 주문 상태 업데이트
        Order updatedOrder = orderControlService.completeOrder(order);

        // 5. 결제 완료
        Payment completedPayment = paymentService.completePayment(command, order);

        // 6. 외부 데이터 플랫폼으로 주문 정보 전송
        try {
            orderEventSender.send(updatedOrder);
        } catch (RuntimeException | InterruptedException e) {
            log.error(ErrorCode.ORDER_SYNC_FAILED.getMessage(), e);
        }
        return PaymentInfo.from(completedPayment);
    }


    public void completeFailedPayment(PaymentCommand command) {
        PaymentInfo.builder()
                .userId(command.getUserId())
                .orderId(command.getOrderId())
                .amount(command.getAmount())
                .status(PaymentStatusType.FAILED)
                .build();
    }
}

