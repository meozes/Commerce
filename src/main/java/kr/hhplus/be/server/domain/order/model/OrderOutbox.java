package kr.hhplus.be.server.domain.order.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    public OrderOutbox(Long userId) {
        this.userId = userId;
        this.status = OutboxStatus.INIT;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
    }
}

