package kr.hhplus.be.server.domain.payment.repository;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PaymentRepository {
    Payment save(Payment payment);

    Payment findByOrderId(Long orderId);

    List<Payment> findAll();

    List<Payment> findByOrderIdIn(Collection<Long> orderIds);
}
