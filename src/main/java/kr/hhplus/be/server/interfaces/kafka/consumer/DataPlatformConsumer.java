package kr.hhplus.be.server.interfaces.kafka.consumer;

import kr.hhplus.be.server.application.event.OrderDataPlatformEvent;
import kr.hhplus.be.server.application.event.OrderEventSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static java.lang.Thread.sleep;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataPlatformConsumer {

    private final OrderEventSender orderEventSender;

    @KafkaListener(topics = "order-data-platform", groupId = "data-platform-group")
    public void consumeOrderDataPlatform(OrderDataPlatformEvent event) {
        try {
            orderEventSender.send(event);
            log.info("[데이터 플랫폼 주문 데이터 처리 완료] orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("[데이터 플랫폼 주문 데이터 처리 실패] orderId={}", event.getOrderId(), e);
        }
    }
}
