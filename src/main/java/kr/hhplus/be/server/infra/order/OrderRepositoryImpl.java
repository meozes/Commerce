package kr.hhplus.be.server.infra.order;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepositoryImpl extends JpaRepository<Order, Long>, OrderRepository {

    @Override
    default Optional<Order> getOrder(Long orderId) {
        return findById(orderId);
    }

    @Override
    default Optional<Order> findById(Long id) {
        return Optional.of(getReferenceById(id));
    }
}
