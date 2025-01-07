package kr.hhplus.be.server.domain.order.repository;

import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import kr.hhplus.be.server.domain.product.entity.Product;

import java.time.LocalDate;
import java.util.List;

public interface OrderItemRepository {
    List<OrderItem> saveAll(List<OrderItem> orderItems);

    List<OrderItem> getOrderItems(Long orderId);

    List<ProductRankInfo> findTopProductsByOrderDate(LocalDate startDate, LocalDate endDate);
}
