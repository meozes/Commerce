package kr.hhplus.be.server.interfaces.balance.response;

import kr.hhplus.be.server.domain.balance.dto.BalanceInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class BalanceResponse {
    private final Long userId;
    private final Integer amount;

    public static BalanceResponse from(BalanceInfo info) {
        return BalanceResponse.builder()
                .userId(info.getUserId())
                .amount(info.getBalance())
                .build();
    }
}
