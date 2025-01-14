package kr.hhplus.be.server.domain.Payment;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import kr.hhplus.be.server.domain.payment.usecase.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    /**
     * 결제 생성 테스트
     */
    @Test
    @DisplayName("결제 생성 성공")
    void createPayment_Success() {
        // given
        Order order = Order.builder()
                .id(1L)
                .userId(1L)
                .originalAmount(10000)
                .finalAmount(10000)
                .discountAmount(0)
                .orderStatus(OrderStatusType.PENDING)
                .build();

        PaymentCommand command = PaymentCommand.builder()
                .userId(1L)
                .orderId(1L)
                .amount(10000)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(order.getId())
                .userId(1L)
                .amount(10000)
                .paymentStatus(PaymentStatusType.PENDING)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // when
        Payment result = paymentService.completePayment(command, order);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusType.PENDING);
        assertThat(result.getAmount()).isEqualTo(command.getAmount());
        assertThat(result.getUserId()).isEqualTo(command.getUserId());
        assertThat(result.getOrderId()).isEqualTo(order.getId());

        verify(paymentRepository).save(any(Payment.class));
    }
}
