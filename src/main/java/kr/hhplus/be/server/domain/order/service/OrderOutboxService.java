package kr.hhplus.be.server.domain.order.service;

import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.infra.kafka.OrderKafkaProducer;
import kr.hhplus.be.server.domain.order.model.OrderOutboxMessage;
import kr.hhplus.be.server.domain.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderOutboxService {
    private final OrderOutboxRepository outboxRepository;
    private final OrderKafkaProducer kafkaProducer;

    @Transactional
    public void saveMessage(OrderCompletedEvent event) {
        OrderOutboxMessage message = OrderOutboxMessage.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .orderStatus(event.getOrderStatus())
                .completedAt(event.getCompletedAt())
                .build();

        outboxRepository.save(message);
    }

    @Transactional
    public void processMessage(OrderOutboxMessage message) {
        try {
            message.updateStatus(OrderOutboxMessage.OutboxStatus.PROCESSING);
            outboxRepository.save(message);

            OrderCompletedEvent event = OrderCompletedEvent.builder()
                    .orderId(message.getOrderId())
                    .userId(message.getUserId())
                    .orderStatus(message.getOrderStatus())
                    .completedAt(message.getCompletedAt())
                    .build();

            kafkaProducer.send(event);

            message.updateStatus(OrderOutboxMessage.OutboxStatus.COMPLETED);
            outboxRepository.save(message);

        } catch (Exception e) {
            log.error("Failed to process message: {}", e.getMessage());
            message.updateStatus(OrderOutboxMessage.OutboxStatus.FAILED);
            outboxRepository.save(message);
            throw e;
        }
    }
}
