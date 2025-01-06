package kr.hhplus.be.server.domain.balance.repository;

import kr.hhplus.be.server.domain.balance.entity.BalanceHistory;
import kr.hhplus.be.server.domain.balance.entity.TransactionType;

public interface BalanceHistoryRepository {
    BalanceHistory saveHistory(Integer beforeBalance, Integer afterBalance, Long balanceId, TransactionType type, Integer amount);
}
