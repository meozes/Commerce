package kr.hhplus.be.server.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import kr.hhplus.be.server.interfaces.payment.request.PaymentRequest;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCommand {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;

    @NotNull(message = "결제 요청 금액은 필수입니다.")
    private Integer amount;

    public static PaymentCommand from(PaymentRequest request) {
        return PaymentCommand.builder()
                .userId(request.getUserId())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .build();
    }
}
