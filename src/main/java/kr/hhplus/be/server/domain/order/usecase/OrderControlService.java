package kr.hhplus.be.server.domain.order.usecase;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderControlService {

    private final OrderRepository orderRepository;

    /**
     * 주문 완료하기
     */
    @Transactional
    public Order completeOrder(Order order) {
        order.complete();
        return orderRepository.save(order);
    }

    /**
     * 주문 취소하기
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order cancelOrder(Order order) {
        try {
            order.canceled();
            return orderRepository.save(order);
        } catch (Exception e) {
            log.error("[주문 취소 실패] orderId={}", order.getId(), e);
            throw e;
        }
    }
}
