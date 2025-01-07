//package kr.hhplus.be.server.application.order;
//
//import kr.hhplus.be.server.domain.coupon.entity.Coupon;
//import kr.hhplus.be.server.domain.coupon.type.CouponStatusType;
//import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
//import kr.hhplus.be.server.domain.order.dto.OrderAmountInfo;
//import kr.hhplus.be.server.domain.order.dto.OrderCommand;
//import kr.hhplus.be.server.domain.order.dto.OrderInfo;
//import kr.hhplus.be.server.domain.order.dto.OrderItemCommand;
//import kr.hhplus.be.server.domain.order.entity.Order;
//import kr.hhplus.be.server.domain.order.entity.OrderItem;
//import kr.hhplus.be.server.domain.order.service.OrderAmountCalculator;
//import kr.hhplus.be.server.domain.order.type.OrderStatusType;
//import kr.hhplus.be.server.domain.order.usecase.OrderService;
//import kr.hhplus.be.server.domain.order.validation.OrderBalanceValidation;
//import kr.hhplus.be.server.domain.order.validation.OrderCouponValidation;
//import kr.hhplus.be.server.domain.order.validation.OrderProductValidation;
//import kr.hhplus.be.server.domain.order.validation.OrderValidation;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDate;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.willThrow;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//class OrderFacadeTest {
//
//    @Mock
//    private OrderService orderService;
//
//    @Mock
//    private OrderBalanceValidation orderBalanceValidation;
//
//    @Mock
//    private OrderCouponValidation orderCouponValidation;
//
//    @Mock
//    private OrderValidation orderValidation;
//
//    @Mock
//    private OrderProductValidation orderProductValidation;
//
//    @Mock
//    private OrderAmountCalculator orderAmountCalculator;
//
//    @InjectMocks
//    private OrderFacade orderFacade;
//
//    private OrderCommand orderCommand;
//    private IssuedCoupon issuedCoupon;
//    private Coupon coupon;
//
//    @BeforeEach
//    void setUp() {
//        // 테스트용 OrderCommand 생성
//        orderCommand = OrderCommand.builder()
//                .userId(1L)
//                .couponId(1L)
//                .orderItems(List.of(
//                        OrderItemCommand.builder()
//                                .productId(1L)
//                                .price(10000)
//                                .quantity(2)
//                                .build()
//                ))
//                .build();
//
//        // 테스트용 Coupon 생성
//        coupon = Coupon.builder()
//                .id(1L)
//                .discountAmount(1000)
//                .dueDate(LocalDate.now().plusDays(7))
//                .build();
//
//        // 테스트용 IssuedCoupon 생성
//        issuedCoupon = IssuedCoupon.builder()
//                .id(1L)
//                .userId(1L)
//                .coupon(coupon)
//                .couponStatus(CouponStatusType.NEW)
//                .build();
//    }
//
//    @Test
//    @DisplayName("잔고, 상품, 쿠폰 validation 통과하여 주문 생성 성공")
//    void createOrder_Success() {
//        // given
//        Order order = Order.builder()
//                .id(1L)
//                .userId(1L)
//                .originalAmount(20000)
//                .discountAmount(1000)
//                .finalAmount(19000)
//                .orderStatus(OrderStatusType.PENDING)
//                .build();
//
//        List<OrderItem> orderItems = List.of(
//                OrderItem.builder()
//                        .id(1L)
//                        .order(order)
//                        .productId(1L)
//                        .productName("테스트 상품")
//                        .quantity(2)
//                        .productPrice(10000)
//                        .totalPrice(20000)
//                        .build()
//        );
//
//        OrderInfo expectedOrderInfo = OrderInfo.of(order, orderItems);
//
//        // OrderAmountInfo Mock 추가
//        OrderAmountInfo amountInfo = OrderAmountInfo.builder()
//                .originalAmount(20000)
//                .discountAmount(1000)
//                .finalAmount(19000)
//                .build();
//
//        given(orderCouponValidation.handleCoupon(orderCommand)).willReturn(issuedCoupon);
//        given(orderAmountCalculator.calculate(eq(orderCommand), any(IssuedCoupon.class))).willReturn(amountInfo);
//        given(orderService.createOrder(any(), any(), any(), any())).willReturn(expectedOrderInfo);
//
//        // when
//        OrderInfo result = orderFacade.createOrder(orderCommand);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getOrder())
//                .hasFieldOrPropertyWithValue("userId", 1L)
//                .hasFieldOrPropertyWithValue("originalAmount", 20000)
//                .hasFieldOrPropertyWithValue("discountAmount", 1000)
//                .hasFieldOrPropertyWithValue("finalAmount", 19000);
//
//        assertThat(result.getOrderItems())
//                .hasSize(1)
//                .element(0)
//                .hasFieldOrPropertyWithValue("productId", 1L)
//                .hasFieldOrPropertyWithValue("quantity", 2)
//                .hasFieldOrPropertyWithValue("productPrice", 10000)
//                .hasFieldOrPropertyWithValue("totalPrice", 20000);
//
//        // verify
//        verify(orderValidation).validateOrder(orderCommand);
//        verify(orderBalanceValidation).handleBalance(orderCommand);
//        verify(orderProductValidation).handleProduct(orderCommand);
//        verify(orderCouponValidation).handleCoupon(orderCommand);
//        verify(orderAmountCalculator).calculate(eq(orderCommand), any(IssuedCoupon.class));
//        verify(orderService).createOrder(
//                orderCommand,
//                20000,
//                1000,
//                19000
//        );
//    }
//
//    @Test
//    @DisplayName("validation 통과하지 못하여 주문 생성 실패")
//    void createOrder_ValidationFails() {
//        // given
//        willThrow(new IllegalArgumentException("주문 검증 실패"))
//                .given(orderValidation)
//                .validateOrder(orderCommand);
//
//        // when & then
//        assertThatThrownBy(() -> {
//            orderFacade.createOrder(orderCommand);
//        })
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("주문 검증 실패");
//    }
//
//    @Test
//    void createOrder_ProductValidationFails() {
//        // given
//        willThrow(new IllegalArgumentException("상품 재고 부족"))
//                .given(orderProductValidation)
//                .handleProduct(orderCommand);
//
//        // when & then
//        assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("상품 재고 부족");
//    }
//}
