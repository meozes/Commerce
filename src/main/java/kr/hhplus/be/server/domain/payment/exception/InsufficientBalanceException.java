package kr.hhplus.be.server.domain.payment.exception;

import kr.hhplus.be.server.domain.payment.dto.PaymentInfo;
import lombok.Getter;


@Getter
public class InsufficientBalanceException extends RuntimeException {
    private PaymentInfo paymentInfo;

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, PaymentInfo paymentInfo) {
        super(message);
        this.paymentInfo = paymentInfo;
    }

}
