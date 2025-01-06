package kr.hhplus.be.server.domain.balance.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BalanceQuery {
    private final Long userId;

    public static BalanceQuery of(Long userId){
        return BalanceQuery.builder()
                .userId(userId)
                .build();
    }
}
