package kr.hhplus.be.server.interfaces.balance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.interfaces.balance.request.ChargeRequest;
import kr.hhplus.be.server.interfaces.balance.response.BalanceResponse;
import kr.hhplus.be.server.interfaces.balance.response.ChargeResponse;
import kr.hhplus.be.server.interfaces.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@Tag(name = "잔고 API", description = "잔고 조회/충전 API")
@RestController
@RequestMapping("api/balance")
public class BalanceController {

    @Operation(summary = "잔고 조회", description = "잔고를 조회합니다.")
    @GetMapping("/{userId}")
    public ApiResponse<BalanceResponse> getBalance(
            @Parameter(name = "userId", description = "사용자 ID", required = true)
            @PathVariable("userId") Long userId
    ) {
        Balance balance = new Balance(1L, userId, 50000);
        return ApiResponse.ok(BalanceResponse.of(balance));
    }

    @Operation(summary = "잔고 충전", description = "잔고를 충전합니다.")
    @PostMapping("/charge")
    public ApiResponse<ChargeResponse> chargeBalance(
            @Parameter(description = "충전 요청 정보")
            @Valid @RequestBody ChargeRequest request
    ) {
        Balance balance = new Balance(1L, request.getUserId(), request.getAmount());
        return ApiResponse.ok(ChargeResponse.of(balance, request.getAmount()));
    }

}
