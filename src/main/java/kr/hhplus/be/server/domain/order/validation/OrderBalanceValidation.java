package kr.hhplus.be.server.domain.order.validation;

import kr.hhplus.be.server.domain.balance.dto.BalanceQuery;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderBalanceValidation {
    private final BalanceService balanceService;

    public void handleBalance(Long userId) {
        balanceService.getBalance(BalanceQuery.of(userId));
    }
}
