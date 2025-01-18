package kr.hhplus.be.server.interfaces.external;


import kr.hhplus.be.server.common.aop.annotation.Retry;
import kr.hhplus.be.server.domain.order.entity.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.TimeoutException;

@Service
public class OrderEventSender {
    /**
     * 외부 데이터 플랫폼으로 전송
     */
    @Retry(
            maxAttempts = 3,
            delay = 1000,
            retryableExceptions = {
                    DataAccessException.class,
                    ResourceAccessException.class,
                    TimeoutException.class
            }
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Order order) throws InterruptedException {
        if (order == null){
            throw new RuntimeException("전송 대상 없음");
        }
    }
}
