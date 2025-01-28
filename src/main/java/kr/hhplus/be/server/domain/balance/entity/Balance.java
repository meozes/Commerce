package kr.hhplus.be.server.domain.balance.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.balance.exception.NotEnoughBalanceException;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Balance extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long userId;

    private Integer balance;


    public void charge(Integer amount) {
        validateChargeAmount(amount);
        this.balance += amount;
    }

    public void deduct(Integer amount) {
        validateDeductAmount(amount);
        validateSufficientBalance(amount);
        this.balance -= amount;
    }

    public void validateChargeAmount(Integer amount) {
        if (amount < 100) {
            throw new IllegalArgumentException(ErrorCode.INVALID_AMOUNT_INPUT.getMessage());
        }
    }

    public void validateDeductAmount(Integer amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_PAYMENT_AMOUNT.getMessage());
        }
    }

    public void validateSufficientBalance(Integer amount) {
        if (this.balance < amount) {
            throw new NotEnoughBalanceException(ErrorCode.INSUFFICIENT_BALANCE.getMessage());
        }
    }
}
