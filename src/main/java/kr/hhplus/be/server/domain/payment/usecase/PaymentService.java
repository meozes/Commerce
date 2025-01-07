package kr.hhplus.be.server.domain.payment.usecase;

import org.springframework.transaction.annotation.Transactional;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createPayment(PaymentCommand command, Order order) {

        Payment payment = Payment.builder()
                .order(order)
                .userId(command.getUserId())
                .amount(command.getAmount())
                .paymentStatus(PaymentStatusType.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        return payment;

    }

    public void completePayment(Payment payment, PaymentStatusType type) {
        payment.complete(type);
        paymentRepository.save(payment);
    }
}
