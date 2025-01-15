package kr.hhplus.be.server.domain.payment.usecase;

import org.springframework.transaction.annotation.Transactional;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 결제 완료
     */
    @Transactional
    public Payment completePayment(PaymentCommand command, Order order) {
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .userId(command.getUserId())
                .amount(command.getAmount())
                .paymentStatus(PaymentStatusType.COMPLETED)
                .build();
        return paymentRepository.save(payment);
    }
}
