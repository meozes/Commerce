package kr.hhplus.be.server.infra.kafka;

import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaProducer {
    private final KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate;
    private static final String TOPIC = "order-completed";

    public void send(OrderCompletedEvent event) {
        ProducerRecord<String, OrderCompletedEvent> record =
                new ProducerRecord<>(TOPIC, String.valueOf(event.getOrderId()), event);

        try {
            kafkaTemplate.send(record).get(10, TimeUnit.SECONDS); // 타임아웃 설정
        } catch (Exception e) {
            log.error("Failed to send message to Kafka: {}", e.getMessage());
            throw new KafkaProducerException(
                    record,
                    "Failed to send message to Kafka",
                    e
            );
        }
    }
}
