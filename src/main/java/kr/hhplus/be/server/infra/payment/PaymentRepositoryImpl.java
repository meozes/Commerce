package kr.hhplus.be.server.infra.payment;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.repository.PaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepositoryImpl extends JpaRepository<Payment, Long>, PaymentRepository {
    @Override
    default Payment save(Payment payment) {
        return saveAndFlush(payment);
    }
}
