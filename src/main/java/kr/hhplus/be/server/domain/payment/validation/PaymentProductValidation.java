package kr.hhplus.be.server.domain.payment.validation;

import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.usecase.OrderService;
import kr.hhplus.be.server.domain.product.usecase.ProductService;
import kr.hhplus.be.server.domain.product.usecase.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentProductValidation {
    private final OrderService orderService;
    private final StockService stockService;

    public void validateAndDeductStock(Long orderId) {
        List<OrderItem> items = orderService.getOrderItems(orderId);
        for (OrderItem item : items) {
            stockService.validateAndDeductStock(item.getProductId(), item.getQuantity());
        }
    }
}
