package kr.hhplus.be.server.domain.order.validation;

import kr.hhplus.be.server.domain.balance.dto.BalanceInfo;
import kr.hhplus.be.server.domain.balance.dto.BalanceQuery;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderBalanceValidation {
    private final BalanceService balanceService;

    public void handleBalance(Long userId) {
        BalanceInfo balance = balanceService.getBalance(BalanceQuery.of(userId));
        if (balance == null) {
            balanceService.createBalance(userId);
        }
    }
}
