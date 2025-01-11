package kr.hhplus.be.server.domain.order.validation;

import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderValidation {
    public void validateOrder(OrderCommand command) {
        if (command.getUserId() < 0) {
            throw new IllegalArgumentException("유효하지 않은 유저 ID 입니다.");
        }
        if (command.getOrderItems() == null || command.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("주문 상품 목록은 필수입니다.");
        }
    }
}
