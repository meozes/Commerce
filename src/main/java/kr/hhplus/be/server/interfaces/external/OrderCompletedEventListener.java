package kr.hhplus.be.server.interfaces.external;

import kr.hhplus.be.server.domain.event.OrderCompletedEvent;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.usecase.OrderFindService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventListener {
    private final OrderEventSender orderEventSender;
    private final OrderFindService orderFindService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Async
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            Order order = orderFindService.getOrder(event.getOrderId()).getOrder();
            orderEventSender.send(order);
            log.info("[주문 완료 데이터 전송 완료] orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("[주문 완료 데이터 전송 실패] orderId={}, error={}",
                    event.getOrderId(),
                    e.getMessage(),
                    e);
        }
    }
}
