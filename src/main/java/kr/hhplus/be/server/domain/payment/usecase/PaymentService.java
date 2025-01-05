package kr.hhplus.be.server.domain.payment.usecase;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import kr.hhplus.be.server.domain.payment.dto.PaymentInfo;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.entity.PaymentStatusType;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentInfo createPayment(PaymentCommand command) {
        if (command.getUserId() == null || command.getUserId() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
        if (command.getOrderId() == null || command.getOrderId() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 주문 ID입니다.");
        }
        if (command.getAmount() == null || command.getAmount() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 결제 금액입니다.");
        }

        Order order = null; //실제로는 OrderRepository에서 찾은 order
        Payment payment = Payment.builder()
                .order(order)
                .userId(command.getUserId())
                .amount(command.getAmount())
                .paymentStatus(PaymentStatusType.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        return PaymentInfo.of(payment);

    }
}
