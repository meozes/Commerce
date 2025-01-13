package kr.hhplus.be.server.domain.order.usecase;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.coupon.dto.IssuedCouponInfo;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
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


    /**
     * 주문 생성하기
     */
    @Transactional
    public OrderInfo createOrder(OrderCommand command, Integer originalAmount, Integer discountAmount, Integer finalAmount, IssuedCoupon issuedCoupon) {
        Order order = Order.builder()
                .userId(command.getUserId())
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .orderStatus(OrderStatusType.PENDING)
                .build();
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = command.getOrderItems().stream()
                .map(item -> OrderItem.builder()
                        .order(savedOrder)
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .productPrice(item.getPrice())
                        .totalPrice(item.getPrice() * item.getQuantity())
                        .build())
                .collect(Collectors.toList());
        orderItems = orderItemRepository.saveAll(orderItems);

        if (issuedCoupon != null){
            issuedCoupon.assignOrderToCoupon(savedOrder);
        }

        return OrderInfo.builder()
                .order(savedOrder)
                .orderItems(orderItems)
                .build();
    }

    /**
     * 주문 완료하기
     */
    @Transactional
    public Order completeOrder(Order order) {
        order.complete();
        return orderRepository.save(order);
    }

    /**
     * 주문 찾기
     */
    public OrderInfo getOrder(Long orderId) {
        return OrderInfo.from(orderRepository.getOrder(orderId));
    }

    /**
     * 주문 아이템 찾기
     */
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.getOrderItems(orderId);
    }

    /**
     * 주문에서 인기상품 찾기
     */
    public List<ProductRankInfo> getTopProductsByOrderDate(LocalDate startDate, LocalDate endDate) {
        return orderItemRepository.findTopProductsByOrderDate(startDate, endDate);
    }
}
