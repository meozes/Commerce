package kr.hhplus.be.server.domain.order.model;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatusType orderStatus;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastRetryAt;

    public enum OutboxStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    @Builder
    public OrderOutboxMessage(Long orderId, Long userId, OrderStatusType orderStatus, LocalDateTime completedAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderStatus = orderStatus;
        this.completedAt = completedAt;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public void updateStatus(OutboxStatus status) {
        this.status = status;
        if (status == OutboxStatus.FAILED) {
            this.retryCount++;
            this.lastRetryAt = LocalDateTime.now();
        }
    }

    public boolean canRetry() {
        return this.retryCount < 3;
    }
}

