package kr.hhplus.be.server.domain.order.repository;

import kr.hhplus.be.server.domain.order.model.OrderOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface OrderOutboxRepository {
    OrderOutbox save(OrderOutbox orderOutbox);

    Optional<OrderOutbox> findByOrderId(Long orderId);

    List<OrderOutbox> findUnpublishedMessages(OrderOutbox.OutboxStatus status,
                                             LocalDateTime threshold);
}
