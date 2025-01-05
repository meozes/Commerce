package kr.hhplus.be.server.interfaces.payment.response;

import kr.hhplus.be.server.domain.payment.dto.PaymentInfo;
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

    public static PaymentResponse of(PaymentInfo payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .build();
    }
}
