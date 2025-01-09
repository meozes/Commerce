package kr.hhplus.be.server.infra.order;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepositoryImpl extends JpaRepository<Order, Long>, OrderRepository {
//    @Override
//    default Order save(Order order) {
//        return saveAndFlush(order);
//    }

    @Override
    default Order getOrder(Long orderId) {
        return findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 주문이 존재하지 않습니다. " + orderId));
    }

    @Override
    default Optional<Order> findById(Long id) {
        return Optional.of(getOne(id));
    }
}
