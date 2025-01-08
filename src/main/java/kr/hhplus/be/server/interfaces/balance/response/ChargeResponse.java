package kr.hhplus.be.server.interfaces.balance.response;

import kr.hhplus.be.server.domain.balance.dto.BalanceInfo;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class ChargeResponse {
    private Long userId;
    private Integer amount;
    private Integer chargedAmount;

    public static ChargeResponse of(BalanceInfo info, Integer chargeAmount){
        return ChargeResponse.builder()
                .userId(info.getUserId())
                .amount(info.getBalance())
                .chargedAmount(chargeAmount)
                .build();
    }
}
