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
public class ChargeResponse {
    private Long userId;
    private Integer balance;
    private Integer chargedAmount;

    public static ChargeResponse of(Balance balance, Integer amount){
        return ChargeResponse.builder()
                .userId(balance.getUserId())
                .balance(balance.getBalance())
                .chargedAmount(amount)
                .build();
    }
}
