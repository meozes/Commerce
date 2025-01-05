package kr.hhplus.be.server.domain.payment.dto;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.entity.PaymentStatusType;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentInfo {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Integer amount;
    private PaymentStatusType status;

    public static PaymentInfo of(Payment payment) {
        return PaymentInfo.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getPaymentStatus())
                .build();
    }
}