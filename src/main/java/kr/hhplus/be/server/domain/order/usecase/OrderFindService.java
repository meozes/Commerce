package kr.hhplus.be.server.domain.order.usecase;

import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import kr.hhplus.be.server.interfaces.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderFindService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 주문 찾기
     */
    public OrderInfo getOrder(Long orderId) {
        return OrderInfo.from(orderRepository.getOrder(orderId)
                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ORDER_NOT_FOUND.getMessage() + orderId)));
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
