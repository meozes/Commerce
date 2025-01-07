package kr.hhplus.be.server.infra.order;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepositoryImpl extends JpaRepository<Order, Long>, OrderRepository {
    @Override
    default Order save(Order order) {
        return saveAndFlush(order);
    }

    @Override
    default Order getOrder(Long orderId) {
        return findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
    }
}
