package kr.hhplus.be.server.domain.order.service;


import kr.hhplus.be.server.domain.order.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventSender {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Order order) throws InterruptedException {
        Thread.sleep(1000);
        if (order == null){
            throw new RuntimeException("전송 대상 없음");
        }
    }
}
