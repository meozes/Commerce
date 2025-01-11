package kr.hhplus.be.server.interfaces.balance.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChargeRequest {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @Min(value = 100, message = "최소 충전금액은 100원입니다.")
    private Integer amount;
}
