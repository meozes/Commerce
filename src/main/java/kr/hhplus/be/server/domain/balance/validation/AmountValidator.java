package kr.hhplus.be.server.domain.balance.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AmountValidator {

    public void validateDeductAmount(Integer amount) {
        if (amount == null) {
            throw new IllegalArgumentException("차감 금액은 필수입니다.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }
    }

    public void validateChargeAmount(Integer amount) {
        if (amount == null) {
            throw new IllegalArgumentException("충전 금액은 필수입니다.");
        }

        if (amount < 100) {
            throw new IllegalArgumentException("충전 금액은 100원 보다 커야 합니다.");
        }
    }
}
