package kr.hhplus.be.server.domain.balance.repository;

import kr.hhplus.be.server.domain.balance.dto.ChargeCommand;
import kr.hhplus.be.server.domain.balance.entity.Balance;

public interface BalanceRepository {
    Balance getBalance(Long userId);

    Balance chargeBalance(ChargeCommand command);

    Balance getBalanceWithLock(Long userId);
}
