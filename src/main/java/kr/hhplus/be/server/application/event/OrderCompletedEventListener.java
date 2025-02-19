package kr.hhplus.be.server.application.event;

import kr.hhplus.be.server.domain.order.event.OrderCompletedEvent;
import kr.hhplus.be.server.domain.order.service.OrderOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventListener {
    private final OrderOutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            outboxService.saveMessage(event);
            log.info("[주문 완료 이벤트 아웃박스 저장 완료] orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("[주문 완료 이벤트 아웃박스 저장 실패] orderId={}, error={}",
                    event.getOrderId(),
                    e.getMessage(),
                    e);
        }
    }
}
