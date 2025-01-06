package kr.hhplus.be.server.domain.balance.usecase;

import org.springframework.transaction.annotation.Transactional;
import kr.hhplus.be.server.domain.balance.dto.BalanceInfo;
import kr.hhplus.be.server.domain.balance.dto.BalanceQuery;
import kr.hhplus.be.server.domain.balance.dto.ChargeCommand;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.type.TransactionType;
import kr.hhplus.be.server.domain.balance.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository historyRepository;

    public BalanceInfo getBalance(BalanceQuery balanceQuery) {
        if (balanceQuery.getUserId() < 0) {
            throw new IllegalArgumentException("유효하지 않은 유저 ID 입니다.");
        }
        Balance balance = balanceRepository.getBalance(balanceQuery.getUserId());
        return BalanceInfo.of(balance);
    }

    public BalanceInfo chargeBalance(ChargeCommand command) {
        if (command.getUserId() < 0) {
            throw new IllegalArgumentException("유효하지 않은 유저 ID 입니다.");
        }
        if (command.getChargeAmount() < 100) {
            throw new IllegalArgumentException("충전 금액은 100원 이상이어야 합니다.");
        }

        Balance original = balanceRepository.getBalanceWithLock(command.getUserId());
        Balance balance = balanceRepository.chargeBalance(command);
        historyRepository.saveHistory(
                original.getBalance(),
                balance.getBalance(),
                balance.getId(),
                TransactionType.CHARGE,
                command.getChargeAmount()
        );
        return BalanceInfo.of(balance);
    }

    public BalanceInfo createBalance(BalanceQuery balanceQuery) {
        Balance balance = balanceRepository.createBalance(balanceQuery.getUserId());
        return BalanceInfo.of(balance);
    }
}
