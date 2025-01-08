package kr.hhplus.be.server.domain.order.validation;

import kr.hhplus.be.server.domain.balance.dto.BalanceInfo;
import kr.hhplus.be.server.domain.balance.dto.BalanceQuery;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderBalanceValidation {
    private final BalanceService balanceService;

    public void handleBalance(OrderCommand command) {
        BalanceInfo balance = balanceService.getBalance(BalanceQuery.of(command.getUserId()));
        if (balance == null) {
            balanceService.createBalance(command.getUserId());
        }
    }
}
