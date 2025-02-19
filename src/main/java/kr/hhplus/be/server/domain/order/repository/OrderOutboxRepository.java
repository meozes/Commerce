package kr.hhplus.be.server.domain.order.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.order.model.OrderOutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutboxMessage, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderOutboxMessage o WHERE o.status = :status AND o.retryCount < :maxRetries")
    List<OrderOutboxMessage> findMessagesForRetry(@Param("status") OrderOutboxMessage.OutboxStatus status,
                                                  @Param("maxRetries") int maxRetries);
}
