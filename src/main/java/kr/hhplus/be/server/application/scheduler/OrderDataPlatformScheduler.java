package kr.hhplus.be.server.application.scheduler;

import org.springframework.transaction.annotation.Transactional;
import kr.hhplus.be.server.application.event.OrderDataPlatformEvent;
import kr.hhplus.be.server.domain.order.model.OrderDataPlatformOutbox;
import kr.hhplus.be.server.domain.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderDataPlatformScheduler {
    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, OrderDataPlatformEvent> kafkaTemplate;
    private static final String TOPIC = "order-data-platform";

    @Scheduled(fixedRate = 300000) // 5분
    @Transactional
    public void rePublishMessages() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<OrderDataPlatformOutbox> unpublishedMessages =
                outboxRepository.findUnpublishedMessages(OrderDataPlatformOutbox.OutboxStatus.INIT, threshold);

        for (OrderDataPlatformOutbox outbox : unpublishedMessages) {
            try {
                OrderDataPlatformEvent event = OrderDataPlatformEvent.builder()
                        .orderId(outbox.getOrderId())
                        .build();

                kafkaTemplate.send(TOPIC, String.valueOf(outbox.getOrderId()), event)
                        .get(10, TimeUnit.SECONDS);

                log.info("[데이터 플랫폼 전송 이벤트 재발행] orderId={}", outbox.getOrderId());
            } catch (Exception e) {
                log.error("[데이터 플랫폼 전송 이벤트 재발행 실패] orderId={}", outbox.getOrderId(), e);
            }
        }
    }
}
