package kr.hhplus.be.server.domain.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOutbox {
    @Id
    private String messageId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum OutboxStatus {
        INIT, PUBLISHED
    }

    @Builder
    public OrderOutbox(String messageId, Long userId, Long orderId) {
        this.messageId = messageId;
        this.userId = userId;
        this.orderId = orderId;
        this.status = OutboxStatus.INIT;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
    }
}

