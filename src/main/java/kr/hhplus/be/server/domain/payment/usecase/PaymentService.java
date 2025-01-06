package kr.hhplus.be.server.domain.payment.usecase;

import jakarta.transaction.Transactional;
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

    @Transactional
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

        //1. 주문 조회
        Order order = null; //실제로는 OrderRepository에서 찾은 order
        Payment payment = Payment.builder()
                .order(order)
                .userId(command.getUserId())
                .amount(command.getAmount())
                .paymentStatus(PaymentStatusType.PENDING)
                .build();

        //2. 결제 생성
        payment = paymentRepository.save(payment);


        //3. 재고 확인, 재고 차감

        //4. 주문 상태 업데이트

        //5. 결제 완료
        return PaymentInfo.from(payment);

    }
}
