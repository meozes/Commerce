package kr.hhplus.be.server.domain.order.repository;

import kr.hhplus.be.server.domain.order.model.OrderDataPlatformOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderDataPlatformOutbox, Long> {

    Optional<OrderDataPlatformOutbox> findByOrderId(Long orderId);

    @Query("SELECT o FROM OrderDataPlatformOutbox o WHERE o.status = :status AND o.createdAt < :threshold")
    List<OrderDataPlatformOutbox> findUnpublishedMessages(
            @Param("status") OrderDataPlatformOutbox.OutboxStatus status,
            @Param("threshold") LocalDateTime threshold
    );
}
