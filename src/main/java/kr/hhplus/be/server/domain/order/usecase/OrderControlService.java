package kr.hhplus.be.server.domain.order.usecase;

import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
        order.canceled();
        return orderRepository.save(order);
    }
}
