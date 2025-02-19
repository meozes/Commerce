package kr.hhplus.be.server.application.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.domain.order.model.OrderOutbox;
import kr.hhplus.be.server.domain.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, OrderSendEvent> kafkaTemplate;
    private static final String TOPIC = "order-data-platform";

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void saveOutbox(OrderCompletedEvent event) {
        try {
            OrderOutbox outbox = OrderOutbox.builder()
                    .userId(event.getUserId())
                    .build();
            outboxRepository.save(outbox);
            log.info("[데이터 플랫폼 전송 아웃박스 저장] userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("[데이터 플랫폼 전송 아웃박스 저장 실패] userId={}", event.getUserId(), e);
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendMessage(OrderCompletedEvent event) {
        try {
            OrderSendEvent dataPlatformEvent = OrderSendEvent.from(event);
            kafkaTemplate.send(TOPIC, String.valueOf(event.getUserId()), dataPlatformEvent)
                    .get(10, TimeUnit.SECONDS);
            log.info("[데이터 플랫폼 전송 이벤트 발행] userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("[데이터 플랫폼 전송 이벤트 발행 실패] userId={}", event.getUserId(), e);
        }
    }
}
