package kr.hhplus.be.server.domain.order.repository;

import kr.hhplus.be.server.domain.product.entity.Product;

import java.time.LocalDate;
import java.util.List;

public interface OrderItemQueryRepository {
    List<Product> findTopProductsByOrderDate(LocalDate startDate, LocalDate endDate);
}
