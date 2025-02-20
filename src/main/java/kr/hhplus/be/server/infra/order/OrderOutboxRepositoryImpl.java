package kr.hhplus.be.server.infra.order;

import kr.hhplus.be.server.domain.order.model.OrderOutbox;
import kr.hhplus.be.server.domain.order.repository.OrderOutboxRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderOutboxRepositoryImpl extends JpaRepository<OrderOutbox, Long>, OrderOutboxRepository {
    @Query("SELECT o FROM OrderOutbox o WHERE o.status = :status AND o.createdAt < :threshold")
    List<OrderOutbox> findUnpublishedMessages(
            @Param("status") OrderOutbox.OutboxStatus status,
            @Param("threshold") LocalDateTime threshold
    );
}
