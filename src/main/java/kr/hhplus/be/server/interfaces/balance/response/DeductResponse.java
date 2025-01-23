package kr.hhplus.be.server.interfaces.balance.response;

import kr.hhplus.be.server.domain.balance.dto.BalanceInfo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeductResponse {
    private Long userId;
    private Integer balance;

    public static DeductResponse from(BalanceInfo info) {
        return DeductResponse.builder()
                .userId(info.getUserId())
                .balance(info.getBalance())
                .build();
    }
}
