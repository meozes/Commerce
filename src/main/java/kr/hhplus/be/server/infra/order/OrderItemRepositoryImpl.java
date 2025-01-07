package kr.hhplus.be.server.infra.order;

import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepositoryImpl extends JpaRepository<OrderItem, Long>, OrderItemRepository {
    @Override
    default List<OrderItem> saveAll(List<OrderItem> orderItems) {
        return saveAllAndFlush(orderItems);
    }

    List<OrderItem> findByOrderId(Long orderId);
}