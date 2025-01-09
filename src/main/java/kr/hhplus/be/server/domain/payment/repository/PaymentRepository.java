package kr.hhplus.be.server.domain.payment.repository;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PaymentRepository {
    Payment save(Payment payment);

    Payment findByOrderId(Long id);

    List<Payment> findAll();

    @Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.order.id = :orderId")
    Payment findByOrderIdWithOrder(@Param("orderId") Long orderId);

    List<Payment> findByOrderIdIn(Collection<Long> orderIds);
}
