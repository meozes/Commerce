package kr.hhplus.be.server.interfaces.scheduler;

import kr.hhplus.be.server.domain.order.model.OrderOutboxMessage;
import kr.hhplus.be.server.domain.order.service.OrderOutboxService;
import kr.hhplus.be.server.domain.order.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderOutboxScheduler {
    private final OrderOutboxService outboxService;
    private final OrderOutboxRepository outboxRepository;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void processFailedMessages() {
        List<OrderOutboxMessage> failedMessages = outboxRepository.findMessagesForRetry(
                OrderOutboxMessage.OutboxStatus.FAILED, 3);

        for (OrderOutboxMessage message : failedMessages) {
            try {
                if (message.canRetry()) {
                    log.info("Retrying failed message: messageId={}, retryCount={}",
                            message.getId(), message.getRetryCount());
                    outboxService.processMessage(message);
                }
            } catch (Exception e) {
                log.error("Retry failed for message: {}", message.getId(), e);
            }
        }
    }
}
