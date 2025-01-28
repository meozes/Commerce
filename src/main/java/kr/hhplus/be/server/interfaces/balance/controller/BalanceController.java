package kr.hhplus.be.server.interfaces.balance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.domain.balance.dto.ChargeCommand;
import kr.hhplus.be.server.domain.balance.dto.BalanceInfo;
import kr.hhplus.be.server.domain.balance.dto.BalanceQuery;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import kr.hhplus.be.server.interfaces.balance.request.ChargeRequest;
import kr.hhplus.be.server.interfaces.balance.request.DeductRequest;
import kr.hhplus.be.server.interfaces.balance.response.BalanceResponse;
import kr.hhplus.be.server.interfaces.balance.response.ChargeResponse;
import kr.hhplus.be.server.interfaces.balance.response.DeductResponse;
import kr.hhplus.be.server.interfaces.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "잔고 API", description = "잔고 조회/충전 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/balance")
public class BalanceController {

    private final BalanceService balanceService;

    @Operation(summary = "잔고 조회", description = "잔고를 조회합니다.")
    @GetMapping("/{userId}")
    public ApiResponse<BalanceResponse> getBalance(
            @Parameter(name = "userId", description = "사용자 ID", required = true)
            @PathVariable("userId") Long userId
    ) {
        BalanceQuery balanceQuery = BalanceQuery.of(userId);
        BalanceInfo info = balanceService.getBalance(balanceQuery);
        return ApiResponse.ok(BalanceResponse.from(info));
    }

    @Operation(summary = "잔고 충전", description = "잔고를 충전합니다.")
    @PostMapping("/charge")
    public ApiResponse<ChargeResponse> chargeBalance(
            @Parameter(description = "충전 요청 정보")
            @Valid @RequestBody ChargeRequest request
    ) {
        ChargeCommand command = ChargeCommand.from(request);
        BalanceInfo info = balanceService.chargeBalance(command);
        return ApiResponse.ok(ChargeResponse.of(info, request.getAmount()));
    }

    @Operation(summary = "잔고 차감", description = "잔고를 차감합니다.")
    @PostMapping("/deduct")
    public ApiResponse<DeductResponse> deductBalance(
            @Parameter(description = "차감 요청 정보")
            @Valid @RequestBody DeductRequest request
    ) {
        BalanceInfo info = balanceService.deductBalance(request.getUserId(), request.getAmount());
        return ApiResponse.ok(DeductResponse.from(info));
    }

}
