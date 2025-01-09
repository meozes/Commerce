package kr.hhplus.be.server.domain.order.repository;

import kr.hhplus.be.server.domain.order.entity.Order;

import java.util.List;

public interface OrderRepository {
    Order save(Order order);

    Order getOrder(Long orderId);

    List<Order> findAll();
}
