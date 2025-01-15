package kr.hhplus.be.server.domain.order.repository;

import kr.hhplus.be.server.domain.order.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> getOrder(Long orderId);

    List<Order> findAll();

    Optional<Order> findById(Long id);
}
