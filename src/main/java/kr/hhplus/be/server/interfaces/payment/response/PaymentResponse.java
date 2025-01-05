package kr.hhplus.be.server.interfaces.payment.response;

import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.entity.PaymentStatusType;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Integer amount;
    private PaymentStatusType status;

    public static PaymentResponse of(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getPaymentStatus())
                .build();
    }
}
