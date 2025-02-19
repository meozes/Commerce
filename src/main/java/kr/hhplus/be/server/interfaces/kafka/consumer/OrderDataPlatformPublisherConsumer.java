package kr.hhplus.be.server.interfaces.kafka.consumer;

import org.springframework.transaction.annotation.Transactional;
import kr.hhplus.be.server.application.event.OrderDataPlatformEvent;
import kr.hhplus.be.server.domain.order.model.OrderDataPlatformOutbox;
import kr.hhplus.be.server.domain.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderDataPlatformPublisherConsumer { // 아웃박스 상태 변경
    private final OrderOutboxRepository outboxRepository;

    @KafkaListener(topics = "order-data-platform", groupId = "order-publisher-group")
    @Transactional
    public void consumeOrderDataPlatform(OrderDataPlatformEvent event) {
        try {
            OrderDataPlatformOutbox outbox = outboxRepository.findByOrderId(event.getOrderId())
                    .orElseThrow(() -> new IllegalStateException("아웃박스에 주문ID 없음 " + event.getOrderId()));

            outbox.markAsPublished();
            outboxRepository.save(outbox);
            log.info("[데이터 플랫폼 전송 이벤트 발행 확인] orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("[데이터 플랫폼 전송 상태 업데이트 실패] orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}
