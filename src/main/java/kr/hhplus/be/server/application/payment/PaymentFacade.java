package kr.hhplus.be.server.application.payment;

import kr.hhplus.be.server.domain.balance.exception.NotEnoughBalanceException;
import kr.hhplus.be.server.domain.order.service.OrderEventSender;
import kr.hhplus.be.server.domain.payment.exception.InsufficientBalanceException;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.usecase.OrderService;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import kr.hhplus.be.server.domain.payment.dto.PaymentInfo;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
import kr.hhplus.be.server.domain.payment.usecase.PaymentService;
import kr.hhplus.be.server.domain.payment.validation.PaymentOrderValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final BalanceService balanceService;
    private final PaymentOrderValidation paymentOrderValidation;
    private final OrderEventSender orderEventSender;

    @Transactional
    public PaymentInfo createPayment(PaymentCommand command) {
        //1. 주문 조회
        Order order = paymentOrderValidation.validateOrder(orderService.getOrder(command.getOrderId()));

        //2. 결제 생성
        Payment payment = paymentService.createPayment(command, order);

        try {

            //3. 잔고 조회, 차감
            balanceService.deductBalance(command.getUserId(), command.getAmount());

            //5. 주문 상태 업데이트
            Order updatedOrder = orderService.completeOrder(order);

            //6. 결제 완료. 결제 상태 업데이트
            Payment completedPayment = paymentService.completePayment(payment, PaymentStatusType.COMPLETED);

            //7. 외부 데이터 플랫폼으로 주문 정보 전송
            try {
                orderEventSender.send(updatedOrder);
            } catch (RuntimeException | InterruptedException e) {
                log.error("데이터 플랫폼 주문 정보 전송 실패", e);
            }
            return PaymentInfo.from(completedPayment);

        } catch (NotEnoughBalanceException e) {
            // 잔고 부족 시 결제 정보 생성 후 실패 상태로 저장
//            PaymentInfo failedPaymentInfo = createFailedPayment(command);
            Payment failedPayment = paymentService.completePayment(payment, PaymentStatusType.FAILED);
            throw new InsufficientBalanceException("잔액이 부족합니다.", PaymentInfo.from(failedPayment));
        } catch (Exception e) {
            // 다른 예외 발생 시
            if (payment != null) {
                Payment failedPayment = paymentService.completePayment(payment, PaymentStatusType.FAILED);
                return PaymentInfo.from(failedPayment);
            }
            throw e;
        }
    }


    protected PaymentInfo createFailedPayment(PaymentCommand command) {
        try {
            Order order = paymentOrderValidation.validateOrder(orderService.getOrder(command.getOrderId()));
            Payment payment = paymentService.createPayment(command, order);
            Payment failedPayment = paymentService.completePayment(payment, PaymentStatusType.FAILED);
            return PaymentInfo.from(failedPayment);
        } catch (Exception e) {
            log.error("실패한 결제 생성 실패", e);
            throw e;
        }
    }
}
