package kr.hhplus.be.server.interfaces.payment.response;

import kr.hhplus.be.server.domain.payment.dto.PaymentInfo;
import kr.hhplus.be.server.domain.payment.type.PaymentStatusType;
import lombok.*;

@Getter
@Builder
public class PaymentResponse {
    private final Long paymentId;
    private final Long orderId;
    private final Long userId;
    private final Integer amount;
    private final PaymentStatusType status;

    public static PaymentResponse from(PaymentInfo payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .build();
    }
}
