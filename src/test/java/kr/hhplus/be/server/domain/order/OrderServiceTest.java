package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.dto.OrderItemCommand;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.order.usecase.OrderService;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    /**
     * 주문 생성 테스트
     */
    @Test
    @DisplayName("쿠폰 없음 - 주문 생성 성공")
    void createOrder_Success() {
        // given
        Long userId = 1L;
        OrderItemCommand orderItemCommand = OrderItemCommand.builder()
                .productId(1L)
                .productName("쿠키")
                .quantity(2)
                .price(10000)
                .build();

        OrderCommand command = OrderCommand.builder()
                .userId(userId)
                .orderItems(List.of(orderItemCommand))
                .couponId(null)
                .build();

        Order order = Order.builder()
                .id(1L)
                .userId(userId)
                .originalAmount(20000)
                .discountAmount(0)
                .finalAmount(20000)
                .orderStatus(OrderStatusType.PENDING)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .order(order)
                .productId(1L)
                .productName("쿠키")
                .quantity(2)
                .productPrice(10000)
                .totalPrice(20000)
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.saveAll(anyList())).thenReturn(List.of(orderItem));

        // when
        OrderInfo result = orderService.createOrder(command, 20000, 0, 20000, null);

        // then
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());

        assertThat(result.getOrder().getUserId()).isEqualTo(userId);
        assertThat(result.getOrder().getFinalAmount()).isEqualTo(20000);
        assertThat(result.getOrderItems()).hasSize(1);

    }

    @Test
    @DisplayName("쿠폰 있음 - 주문 생성 성공")
    void createOrder_withCoupon() {
        // given
        OrderCommand command = OrderCommand.builder()
                .userId(1L)
                .orderItems(List.of(OrderItemCommand.builder()
                        .productId(1L)
                        .productName("쿠키")
                        .quantity(2)
                        .price(10000)
                        .build()))
                .couponId(1L)
                .build();

        Order order = Order.builder()
                .id(1L)
                .userId(1L)
                .originalAmount(20000)
                .discountAmount(2000)
                .finalAmount(18000)
                .orderStatus(OrderStatusType.PENDING)
                .build();

        IssuedCoupon coupon = mock(IssuedCoupon.class);

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.saveAll(anyList())).thenReturn(List.of(mock(OrderItem.class)));

        // when
        OrderInfo result = orderService.createOrder(command, 20000, 2000, 18000, coupon);

        // then
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
        verify(coupon).assignToOrder(any(Order.class));

        assertThat(result.getOrder().getDiscountAmount()).isEqualTo(2000);
        assertThat(result.getOrder().getFinalAmount()).isEqualTo(18000);

    }


    /**
     * 주문 완료 처리 테스트
     */
    @Test
    @DisplayName("주문 완료 처리 성공")
    void completeOrder_Success() {
        // given
        Order order = Order.builder()
                .id(1L)
                .userId(1L)
                .originalAmount(20000)
                .discountAmount(0)
                .finalAmount(20000)
                .orderStatus(OrderStatusType.PENDING)
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        Order completedOrder = orderService.completeOrder(order);

        // then
        assertThat(completedOrder.getOrderStatus()).isEqualTo(OrderStatusType.COMPLETED);
        verify(orderRepository).save(order);
    }


    /**
     * 오더에서 상품 판매 순위 조회 테스트
     */
    @Test
    @DisplayName("상품 판매 순위 조회 성공")
    void getTopProductsByOrderDate_Success() {
        // given
        LocalDate startDate = LocalDate.now().minusDays(4);
        LocalDate endDate = LocalDate.now().minusDays(1);

        List<ProductRankInfo> mockRankList = List.of(
                new ProductRankInfo(1L, "우유", 100L, 10000),
                new ProductRankInfo(2L, "쿠키", 80L, 15000)
        );

        when(orderItemRepository.findTopProductsByOrderDate(startDate, endDate))
                .thenReturn(mockRankList);

        // when
        List<ProductRankInfo> result = orderService.getTopProductsByOrderDate(startDate, endDate);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProductName()).isEqualTo("우유");
        assertThat(result.get(0).getTotalQuantitySold()).isEqualTo(100);
        verify(orderItemRepository).findTopProductsByOrderDate(startDate, endDate);
    }



}
