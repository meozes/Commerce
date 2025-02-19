package kr.hhplus.be.server.application.event;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.domain.order.model.OrderDataPlatformOutbox;
import kr.hhplus.be.server.domain.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDataPlatformEventListener {
    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, OrderDataPlatformEvent> kafkaTemplate;
    private static final String TOPIC = "order-data-platform";

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional
    public void saveOutbox(OrderCompletedEvent event) {
        try {
            OrderDataPlatformOutbox outbox = OrderDataPlatformOutbox.builder()
                    .orderId(event.getOrderId())
                    .build();
            outboxRepository.save(outbox);
            log.info("[데이터 플랫폼 전송 아웃박스 저장] orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("[데이터 플랫폼 전송 아웃박스 저장 실패] orderId={}", event.getOrderId(), e);
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendMessage(OrderCompletedEvent event) {
        try {
            OrderDataPlatformEvent dataPlatformEvent = OrderDataPlatformEvent.from(event);
            kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), dataPlatformEvent)
                    .get(10, TimeUnit.SECONDS);
            log.info("[데이터 플랫폼 전송 이벤트 발행] orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("[데이터 플랫폼 전송 이벤트 발행 실패] orderId={}", event.getOrderId(), e);
        }
    }
}
