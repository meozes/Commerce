package kr.hhplus.be.server.infra.balance;

import kr.hhplus.be.server.domain.balance.entity.BalanceHistory;
import kr.hhplus.be.server.domain.balance.type.TransactionType;
import kr.hhplus.be.server.domain.balance.repository.BalanceHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceHistoryRepositoryImpl extends JpaRepository<BalanceHistory, Long>, BalanceHistoryRepository {
    @Override
    default BalanceHistory saveHistory(Integer beforeBalance, Integer afterBalance, Long balanceId, TransactionType type, Integer amount) {
        BalanceHistory history = BalanceHistory.builder()
                .balanceId(balanceId)
                .type(type)
                .amount(amount)
                .totalAmount(afterBalance)
                .build();
        return save(history);
    }
}
