package kr.hhplus.be.server.domain.balance.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import kr.hhplus.be.server.domain.payment.exception.InsufficientBalanceException;
import lombok.*;
import org.springframework.data.annotation.Version;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Balance extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Integer balance;

    @Version
    private Long version;


    public void charge(Integer amount) {
        validateChargeAmount(amount);
        this.balance += amount;
    }

    public void deduct(Integer amount) {
        validateDeductAmount(amount);
        validateSufficientBalance(amount);
        this.balance -= amount;
    }

    private void validateChargeAmount(Integer amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 100원 보다 커야 합니다.");
        }
    }

    private void validateDeductAmount(Integer amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
    }

    private void validateSufficientBalance(Integer amount) {
        if (this.balance < amount) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }
    }
}
