//package kr.hhplus.be.server.application.payment;
//
//import kr.hhplus.be.server.domain.balance.dto.BalanceInfo;
//import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
//import kr.hhplus.be.server.domain.order.entity.Order;
//import kr.hhplus.be.server.domain.order.type.OrderStatusType;
//import kr.hhplus.be.server.domain.order.usecase.OrderService;
//import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
//import kr.hhplus.be.server.domain.payment.dto.PaymentInfo;
//import kr.hhplus.be.server.domain.payment.entity.Payment;
//import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
//import kr.hhplus.be.server.domain.payment.usecase.PaymentService;
//import kr.hhplus.be.server.domain.payment.validation.PaymentOrderValidation;
//import kr.hhplus.be.server.domain.payment.validation.PaymentProductValidation;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PaymentFacadeTest {
//    @Mock
//    private OrderService orderService;
//    @Mock
//    private PaymentService paymentService;
//    @Mock
//    private BalanceService balanceService;
//    @Mock
//    private PaymentOrderValidation paymentOrderValidation;
//    @Mock
//    private PaymentProductValidation paymentProductValidation;
//
//    @InjectMocks
//    private PaymentFacade paymentFacade;
//
//    @Test
//    @DisplayName("결제 성공 테스트")
//    void createPayment_Success() {
//        // Given
//        PaymentCommand command = PaymentCommand.builder()
//                .orderId(1L)
//                .userId(1L)
//                .amount(10000)
//                .build();
//
//        Order order = Order.builder()
//                .id(1L)
//                .userId(1L)
//                .finalAmount(10000)
//                .orderStatus(OrderStatusType.PENDING)
//                .build();
//
//        Payment payment = Payment.builder()
//                .id(1L)
//                .order(order)
//                .amount(10000)
//                .build();
//
//        BalanceInfo balanceInfo = BalanceInfo.builder()
//                .balance(5000)  // 차감 후 잔액
//                .build();
//
//        when(orderService.getOrder(command.getOrderId())).thenReturn(order);
//        when(paymentOrderValidation.validateOrder(order)).thenReturn(order);
//        when(paymentService.createPayment(command, order)).thenReturn(payment);
//        doNothing().when(paymentProductValidation).validateAndDeductStock(order.getId());
//        when(balanceService.deductBalance(command.getUserId(), command.getAmount())).thenReturn(balanceInfo);
//        when(orderService.completeOrder(order)).thenReturn(order);
//        doNothing().when(paymentService).completePayment(payment, PaymentStatusType.COMPLETED);
//
//        // When
//        PaymentInfo result = paymentFacade.createPayment(command);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getPaymentId()).isEqualTo(payment.getId());
//
//        verify(orderService).getOrder(command.getOrderId());
//        verify(paymentOrderValidation).validateOrder(order);
//        verify(paymentService).createPayment(command, order);
//        verify(paymentProductValidation).validateAndDeductStock(order.getId());
//        verify(balanceService).deductBalance(command.getUserId(), command.getAmount());
//        verify(orderService).completeOrder(order);
//        verify(paymentService).completePayment(payment, PaymentStatusType.COMPLETED);
//    }
//
//    @Test
//    @DisplayName("주문 검증 실패 테스트")
//    void createPayment_WhenOrderValidationFails_ThrowsException() {
//        // Given
//        PaymentCommand command = PaymentCommand.builder()
//                .orderId(1L)
//                .build();
//
//        Order order = Order.builder()
//                .id(1L)
//                .orderStatus(OrderStatusType.COMPLETED)
//                .build();
//
//        when(orderService.getOrder(command.getOrderId())).thenReturn(order);
//        when(paymentOrderValidation.validateOrder(order))
//                .thenThrow(new IllegalArgumentException("주문이 이미 완료되었습니다."));
//
//        // When & Then
//        assertThrows(IllegalArgumentException.class,
//                () -> paymentFacade.createPayment(command));
//
//        verify(orderService).getOrder(command.getOrderId());
//        verify(paymentOrderValidation).validateOrder(order);
//        verifyNoInteractions(paymentService, paymentProductValidation, balanceService);
//    }
//
//    @Test
//    @DisplayName("상품 재고 부족 테스트")
//    void createPayment_WhenProductStockNotAvailable_ThrowsException() {
//        // Given
//        PaymentCommand command = PaymentCommand.builder()
//                .orderId(1L)
//                .userId(1L)
//                .amount(10000)
//                .build();
//
//        Order order = Order.builder()
//                .id(1L)
//                .userId(1L)
//                .finalAmount(10000)
//                .orderStatus(OrderStatusType.PENDING)
//                .build();
//
//        Payment payment = Payment.builder()
//                .id(1L)
//                .order(order)
//                .amount(10000)
//                .build();
//
//        when(orderService.getOrder(command.getOrderId())).thenReturn(order);
//        when(paymentOrderValidation.validateOrder(order)).thenReturn(order);
//        when(paymentService.createPayment(command, order)).thenReturn(payment);
//        doThrow(new IllegalStateException("재고가 부족합니다."))
//                .when(paymentProductValidation).validateAndDeductStock(order.getId());
//
//        // When & Then
//        assertThrows(IllegalStateException.class,
//                () -> paymentFacade.createPayment(command));
//
//        verify(orderService).getOrder(command.getOrderId());
//        verify(paymentOrderValidation).validateOrder(order);
//        verify(paymentService).createPayment(command, order);
//        verify(paymentProductValidation).validateAndDeductStock(order.getId());
//        verifyNoInteractions(balanceService);
//        verify(orderService, never()).completeOrder(any());
//        verify(paymentService, never()).completePayment(payment,PaymentStatusType.FAILED);
//    }
//
//    @Test
//    @DisplayName("잔액 부족 테스트")
//    void createPayment_WhenInsufficientBalance_ThrowsException() {
//        // Given
//        PaymentCommand command = PaymentCommand.builder()
//                .orderId(1L)
//                .userId(1L)
//                .amount(10000)
//                .build();
//
//        Order order = Order.builder()
//                .id(1L)
//                .userId(1L)
//                .finalAmount(10000)
//                .orderStatus(OrderStatusType.PENDING)
//                .build();
//
//        Payment payment = Payment.builder()
//                .id(1L)
//                .order(order)
//                .amount(10000)
//                .build();
//
//        when(orderService.getOrder(command.getOrderId())).thenReturn(order);
//        when(paymentOrderValidation.validateOrder(order)).thenReturn(order);
//        when(paymentService.createPayment(command, order)).thenReturn(payment);
//        doNothing().when(paymentProductValidation).validateAndDeductStock(order.getId());
//        doThrow(new IllegalStateException("잔액이 부족합니다."))
//                .when(balanceService).deductBalance(command.getUserId(), command.getAmount());
//
//        // When & Then
//        assertThrows(IllegalStateException.class,
//                () -> paymentFacade.createPayment(command));
//
//        verify(orderService).getOrder(command.getOrderId());
//        verify(paymentOrderValidation).validateOrder(order);
//        verify(paymentService).createPayment(command, order);
//        verify(paymentProductValidation).validateAndDeductStock(order.getId());
//        verify(balanceService).deductBalance(command.getUserId(), command.getAmount());
//        verify(orderService, never()).completeOrder(any());
//        verify(paymentService, never()).completePayment(payment, PaymentStatusType.FAILED);
//    }
//
//}
