package kr.hhplus.be.server.application.payment;

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
import kr.hhplus.be.server.domain.payment.validation.PaymentProductValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final BalanceService balanceService;
    private final PaymentOrderValidation paymentOrderValidation;
    private final PaymentProductValidation paymentProductValidation;

    @Transactional
    public PaymentInfo createPayment(PaymentCommand command) {
        Payment payment = null;

        try {
            //1. 주문 조회
            Order order = paymentOrderValidation.validateOrder(orderService.getOrder(command.getOrderId()));

            //2. 결제 생성
            payment = paymentService.createPayment(command, order);

            //3. 재고 확인, 재고 차감
            paymentProductValidation.validateAndDeductStock(order.getId());

            //4. 잔고 조회, 차감
            balanceService.deductBalance(command.getUserId(), command.getAmount());

            //5. 주문 상태 업데이트
            orderService.completeOrder(order);

            //6. 결제 완료. 결제 상태 업데이트
            paymentService.completePayment(payment, PaymentStatusType.COMPLETED);

            return PaymentInfo.from(payment);
        } catch (InsufficientBalanceException e) {
            if (payment != null){
                paymentService.completePayment(payment, PaymentStatusType.FAILED);
            }
            throw e;
        }

    }
}
