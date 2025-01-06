package kr.hhplus.be.server.domain.payment.dto;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.entity.PaymentStatusType;
import lombok.*;

@Getter
@Builder
public class PaymentInfo {
    private final Long paymentId;
    private final Long orderId;
    private final Long userId;
    private final Integer amount;
    private final PaymentStatusType status;

    public static PaymentInfo from(Payment payment) {
        return PaymentInfo.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getPaymentStatus())
                .build();
    }
}