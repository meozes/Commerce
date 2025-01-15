package kr.hhplus.be.server.interfaces.external;


import kr.hhplus.be.server.domain.order.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEventSender {
    /**
     * 외부 데이터 플랫폼으로 전송
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Order order) throws InterruptedException {
        Thread.sleep(1000);
        if (order == null){
            throw new RuntimeException("전송 대상 없음");
        }
    }
}
