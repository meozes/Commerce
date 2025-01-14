package kr.hhplus.be.server.domain.order.validation;

import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderValidator {
    public void validateOrder(OrderCommand command) {
        if (command.getUserId() < 0) {
            throw new IllegalArgumentException("유효하지 않은 유저 ID 입니다.");
        }
        if (command.getOrderItems() == null || command.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("주문 상품 목록은 필수입니다.");
        }
    }

    public Order validateOrder(OrderInfo order, PaymentCommand command) {
        if (order == null) {
            throw new IllegalArgumentException("주문이 존재하지 않습니다.");
        }
        if (!order.getOrder().getOrderStatus().equals(OrderStatusType.PENDING)) {
            throw new IllegalArgumentException("주문이 취소되었거나 처리 완료되었습니다.");
        }
        if (!order.getOrder().getFinalAmount().equals(command.getAmount())){
            throw new IllegalArgumentException("결제 금액이 불일치합니다.");
        }
        return order.getOrder();
    }
}
