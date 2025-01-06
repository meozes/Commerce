package kr.hhplus.be.server.domain.Payment;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderStatusType;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import kr.hhplus.be.server.domain.payment.dto.PaymentInfo;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.entity.PaymentStatusType;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.domain.payment.usecase.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 생성 성공")
    void createPayment_Success() {
        // given
        PaymentCommand command = new PaymentCommand(1L, 1L, 45000);

        Order order = Order.builder()
                .id(1L)
                .userId(1L)
                .finalAmount(45000)
                .orderStatus(OrderStatusType.PENDING)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .userId(1L)
                .amount(45000)
                .paymentStatus(PaymentStatusType.PENDING)
                .build();

        when(paymentRepository.save(any())).thenReturn(payment);

        // when
        PaymentInfo result = paymentService.createPayment(command);

        // then
        assertThat(result.getPaymentId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualTo(45000);
        assertThat(result.getStatus()).isEqualTo(PaymentStatusType.PENDING);
    }

    @Test
    @DisplayName("유효하지 않은 사용자 ID - 결제 실패")
    void createPayment_InvalidUserId() {
        // given
        PaymentCommand command = new PaymentCommand(-1L, 1L, 45000);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(command));
    }

    @Test
    @DisplayName("유효하지 않은 주문 ID - 결제 실패")
    void createPayment_InvalidOrderId() {
        // given
        PaymentCommand command = PaymentCommand.builder()
                .userId(1L)
                .orderId(-1L)
                .amount(45000)
                .build();

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(command),
                "유효하지 않은 주문 ID입니다.");
    }

    @Test
    @DisplayName("유효하지 않은 결제 금액 - 결제 실패")
    void createPayment_InvalidAmount() {
        // given
        PaymentCommand command = PaymentCommand.builder()
                .userId(1L)
                .orderId(1L)
                .amount(-1000)
                .build();

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.createPayment(command),
                "유효하지 않은 결제 금액입니다.");
    }

}
