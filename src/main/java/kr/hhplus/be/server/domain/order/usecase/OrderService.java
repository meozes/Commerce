package kr.hhplus.be.server.domain.order.usecase;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import kr.hhplus.be.server.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;


    @Transactional
    public OrderInfo createOrder(OrderCommand command, Integer originalAmount, Integer discountAmount, Integer finalAmount) {

        Order order = Order.builder()
                .userId(command.getUserId())
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .orderStatus(OrderStatusType.PENDING)
                .build();
        Order saveOrder = orderRepository.save(order);

        List<OrderItem> orderItems = command.getOrderItems().stream()
                .map(item -> OrderItem.builder()
                        .order(saveOrder)
                        .productId(item.getProductId())
                        .productName(item.getProductName())
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

    public Order getOrder(Long orderId) {
        return orderRepository.getOrder(orderId);
    }

    @Transactional
    public Order completeOrder(Order order) {
        order.complete();
        return orderRepository.save(order);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.getOrderItems(orderId);
    }

    public List<ProductRankInfo> getTopProductsByOrderDate(LocalDate startDate, LocalDate endDate) {
        return orderItemRepository.findTopProductsByOrderDate(startDate, endDate);
    }
}
