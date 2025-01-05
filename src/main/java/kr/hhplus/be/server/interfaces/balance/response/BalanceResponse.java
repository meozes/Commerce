package kr.hhplus.be.server.interfaces.balance.response;

import kr.hhplus.be.server.domain.balance.entity.Balance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BalanceResponse {
    private Long userId;
    private Integer balance;

    public static BalanceResponse of(Balance balance) {
        return BalanceResponse.builder()
                .userId(balance.getUserId())
                .balance(balance.getBalance())
                .build();
    }
}
