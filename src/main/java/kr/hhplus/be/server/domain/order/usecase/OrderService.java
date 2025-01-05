package kr.hhplus.be.server.domain.order.usecase;

import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.entity.OrderStatusType;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderInfo createOrder(OrderCommand command) {

        if (command.getUserId() < 0) {
            throw new IllegalArgumentException("유효하지 않은 유저 ID 입니다.");
        }
        if (command.getOrderItems() == null || command.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("주문 상품 목록은 필수입니다.");
        }

        Integer originalAmount = command.getOrderItems().stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        Integer discountAmount = 0; // 실제로는 쿠폰 서비스를 통해 계산
        Integer finalAmount = originalAmount - discountAmount; // 최종금액

        Order order = Order.builder()
                .userId(command.getUserId())
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .orderStatus(OrderStatusType.PENDING)
                .build();
        Order saveOrder = orderRepository.save(order);

        String productName = ""; // 실제로는 상품 서비스를 통해 계산
        List<OrderItem> orderItems = command.getOrderItems().stream()
                .map(item -> OrderItem.builder()
                        .order(saveOrder)
                        .productId(item.getProductId())
                        .productName(productName)
                        .quantity(item.getQuantity())
                        .productPrice(item.getPrice())
                        .totalPrice(item.getPrice() * item.getQuantity())
                        .build())
                .collect(Collectors.toList());
        orderItems = orderItemRepository.saveAll(orderItems);

        return OrderInfo.builder()
                .order(saveOrder)
                .orderItems(orderItems)
                .build();
    }
}
