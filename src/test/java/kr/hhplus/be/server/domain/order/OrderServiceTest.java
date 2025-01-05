package kr.hhplus.be.server.domain.order;

import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.entity.OrderStatusType;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.order.usecase.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
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

    private OrderCommand createOrderCommand(Long userId, Long couponId,
                                            List<OrderCommand.OrderItemCommand> orderItems) {
        return OrderCommand.builder()
                .userId(userId)
                .orderItems(orderItems)
                .couponId(couponId)
                .build();
    }

    private Order createOrder(Long id, Long userId, Integer originalAmount,
                              Integer discountAmount, OrderStatusType orderStatus) {
        return Order.builder()
                .id(id)
                .userId(userId)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(originalAmount - discountAmount)
                .orderStatus(orderStatus)
                .build();
    }

    private List<OrderItem> createOrderItems(List<OrderItemDto> itemDtos, Order order) {
        return itemDtos.stream()
                .map(dto -> OrderItem.builder()
                        .id(dto.id())
                        .order(order)
                        .productId(dto.productId())
                        .productName(dto.productName())
                        .quantity(dto.quantity())
                        .productPrice(dto.price())
                        .totalPrice(dto.price() * dto.quantity())
                        .build())
                .collect(Collectors.toList());
    }

    private record OrderItemDto(
            Long id,
            Long productId,
            String productName,
            int quantity,
            int price
    ) {
    }


    @Test
    @DisplayName("쿠폰 없음 - 주문 생성 성공")
    void createOrder_Success() {
        // given
        List<OrderCommand.OrderItemCommand> commandItems = List.of(
                new OrderCommand.OrderItemCommand(1L, 2, 10000),
                new OrderCommand.OrderItemCommand(2L, 1, 15000)
        );
        OrderCommand command = createOrderCommand(1L, null, commandItems);

        Order order = createOrder(1L, 1L, 35000, 0, OrderStatusType.PENDING);

        List<OrderItemDto> itemDtos = List.of(
                new OrderItemDto(1L, 1L, "쿠키", 2, 10000),
                new OrderItemDto(2L, 2L, "우유", 1, 15000)
        );
        List<OrderItem> orderItems = createOrderItems(itemDtos, order);

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.saveAll(anyList())).thenReturn(orderItems);

        // when
        OrderInfo result = orderService.createOrder(command);

        // then
        assertAll(
                () -> assertEquals(1L, result.getOrder().getId()),
                () -> assertEquals(35000, result.getOrder().getOriginalAmount()),
                () -> assertEquals(0, result.getOrder().getDiscountAmount()),
                () -> assertEquals(35000, result.getOrder().getFinalAmount()),
                () -> assertEquals(OrderStatusType.PENDING, result.getOrder().getOrderStatus()),
                () -> assertEquals(2, result.getOrderItems().size()),
                () -> assertEquals(20000, result.getOrderItems().get(0).getTotalPrice()),
                () -> assertEquals(15000, result.getOrderItems().get(1).getTotalPrice())
        );

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());

    }

    @Test
    @DisplayName("쿠폰 있음 - 주문 생성 성공")
    void createOrder_withCoupon() {
        // given
        List<OrderCommand.OrderItemCommand> commandItems = List.of(
                new OrderCommand.OrderItemCommand(1L, 2, 10000),
                new OrderCommand.OrderItemCommand(2L, 1, 15000)
        );
        OrderCommand command = createOrderCommand(1L, 1L, commandItems); // couponId 1L 추가

        Order order = createOrder(1L, 1L, 35000, 3500, OrderStatusType.PENDING); // 10% 할인 적용

        List<OrderItemDto> itemDtos = List.of(
                new OrderItemDto(1L, 1L, "쿠키", 2, 10000),
                new OrderItemDto(2L, 2L, "우유", 1, 15000)
        );
        List<OrderItem> orderItems = createOrderItems(itemDtos, order);

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.saveAll(anyList())).thenReturn(orderItems);

        // when
        OrderInfo result = orderService.createOrder(command);

        // then
        assertAll(
                () -> assertEquals(1L, result.getOrder().getId()),
                () -> assertEquals(35000, result.getOrder().getOriginalAmount()),
                () -> assertEquals(3500, result.getOrder().getDiscountAmount()),
                () -> assertEquals(31500, result.getOrder().getFinalAmount()),
                () -> assertEquals(OrderStatusType.PENDING, result.getOrder().getOrderStatus()),
                () -> assertEquals(2, result.getOrderItems().size()),
                () -> assertEquals(20000, result.getOrderItems().get(0).getTotalPrice()),
                () -> assertEquals(15000, result.getOrderItems().get(1).getTotalPrice())
        );

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());

    }

    @Test
    @DisplayName("상품 목록 없음 - 주문 생성 실패")
    void createOrder_emptyOrderItems() {
        // given
        List<OrderCommand.OrderItemCommand> emptyCommandItems = List.of();
        OrderCommand command = createOrderCommand(1L, null, emptyCommandItems);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(command);
        });

        // verify
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(anyList());

    }

    @Test
    @DisplayName("userId < 0 - 주문 생성 실패")
    void createOrder_invalidUserId() {
        // given
        List<OrderCommand.OrderItemCommand> commandItems = List.of(
                new OrderCommand.OrderItemCommand(1L, 2, 10000),
                new OrderCommand.OrderItemCommand(2L, 1, 15000)
        );
        OrderCommand command = createOrderCommand(-1L, null, commandItems); // 음수 userId

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(command);
        });

        // verify
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(anyList());
    }

}
