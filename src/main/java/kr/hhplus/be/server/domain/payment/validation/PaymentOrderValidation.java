package kr.hhplus.be.server.domain.payment.validation;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.order.usecase.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentOrderValidation {
    private final OrderService orderService;

    public Order validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("주문이 존재하지 않습니다.");
        }
        if (!order.getOrderStatus().equals(OrderStatusType.PENDING)) {
            throw new IllegalArgumentException("주문이 취소되었거나 처리 완료되었습니다.");
        }
        return order;
    }
}
