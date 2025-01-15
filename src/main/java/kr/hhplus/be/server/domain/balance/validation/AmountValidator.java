package kr.hhplus.be.server.domain.balance.validation;

import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AmountValidator {

    public void validateDeductAmount(Integer amount) {
        if (amount == null) {
            throw new IllegalArgumentException(ErrorCode.DEDUCTION_AMOUNT_REQUIRED.getMessage());
        }

        if (amount <= 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_DEDUCTION_AMOUNT.getMessage());
        }
    }

    public void validateChargeAmount(Integer amount) {
        if (amount == null) {
            throw new IllegalArgumentException(ErrorCode.CHARGE_AMOUNT_REQUIRED.getMessage());
        }

        if (amount < 100) {
            throw new IllegalArgumentException(ErrorCode.INVALID_AMOUNT_INPUT.getMessage());
        }
    }
}
