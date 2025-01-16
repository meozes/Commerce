package kr.hhplus.be.server.domain.order.usecase;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.aop.annotation.Monitored;
import kr.hhplus.be.server.common.aop.annotation.Monitoring;
import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 주문 생성하기
     */
    @Monitored
    @Monitoring
    @Transactional
    public OrderInfo createOrder(OrderCommand command, Integer originalAmount, Integer discountAmount, Integer finalAmount, IssuedCoupon issuedCoupon) {

        log.info("[주문 생성 시작] userId={}, originalAmount={}, discountAmount={}, finalAmount={}, hasCoupon={}",
                command.getUserId(),
                originalAmount,
                discountAmount,
                finalAmount,
                issuedCoupon != null);

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

        log.info("[주문 생성 완료] orderId={}, userId={}, orderItems={}, originalAmount={}, discountAmount={}, finalAmount={}",
                savedOrder.getId(),
                command.getUserId(),
                orderItems.stream()
                        .map(item -> String.format("상품ID:%d, 수량:%d", item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList()),
                originalAmount,
                discountAmount,
                finalAmount);

        return OrderInfo.builder()
                .order(savedOrder)
                .orderItems(orderItems)
                .build();
    }
}
