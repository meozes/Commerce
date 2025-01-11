package kr.hhplus.be.server.domain.balance.dto;

import kr.hhplus.be.server.interfaces.balance.request.ChargeRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChargeCommand {
    private final Long userId;
    private final Integer chargeAmount;

    public static ChargeCommand from(ChargeRequest request){
        return ChargeCommand.builder()
                .userId(request.getUserId())
                .chargeAmount(request.getAmount())
                .build();
    }
}
