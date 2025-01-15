package kr.hhplus.be.server.domain.order.validation;

import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import kr.hhplus.be.server.interfaces.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderValidator {
    public void validateOrder(OrderCommand command) {
        if (command.getUserId() < 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_USER_ID.getMessage());
        }
        if (command.getOrderItems() == null || command.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.ORDER_ITEMS_REQUIRED.getMessage());
        }
    }

    public Order validateOrder(OrderInfo order, PaymentCommand command) {
        if (order == null) {
            throw new NoSuchElementException(ErrorCode.ORDER_ITEMS_REQUIRED.getMessage());
        }
        if (!order.getOrder().getOrderStatus().equals(OrderStatusType.PENDING)) {
            throw new IllegalStateException(ErrorCode.ORDER_ALREADY_COMPLETED.getMessage());
        }
        if (!order.getOrder().getFinalAmount().equals(command.getAmount())){
            throw new IllegalStateException(ErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());
        }
        return order.getOrder();
    }
}
