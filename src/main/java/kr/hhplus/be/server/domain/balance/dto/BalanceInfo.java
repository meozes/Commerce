package kr.hhplus.be.server.domain.balance.dto;

import kr.hhplus.be.server.domain.balance.entity.Balance;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BalanceInfo {
    private final Long userId;
    private final Integer balance;

    public static BalanceInfo from(Balance balance){
        return BalanceInfo.builder()
                .userId(balance.getUserId())
                .balance(balance.getBalance())
                .build();
    }
}
