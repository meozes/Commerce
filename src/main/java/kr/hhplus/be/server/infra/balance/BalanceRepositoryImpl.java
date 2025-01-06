package kr.hhplus.be.server.infra.balance;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.balance.dto.ChargeCommand;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BalanceRepositoryImpl extends JpaRepository<Balance, Long>, BalanceRepository {
    @Override
    default Balance getBalance(Long userId) {
        return findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저 ID에 해당하는 잔고가 없습니다. " + userId));
    }

    @Override
    default Balance chargeBalance(ChargeCommand command) {
        Balance balance = getBalance(command.getUserId());
        balance = Balance.builder()
                .id(balance.getId())
                .userId(balance.getUserId())
                .balance(balance.getBalance() + command.getChargeAmount())
                .build();
        return save(balance);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Balance b where b.userId = :userId")
    Optional<Balance> findByUserIdWithLock(@Param("userId") Long userId);

    @Override
    default Balance getBalanceWithLock(Long userId) {
        return findByUserIdWithLock(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저 ID에 해당하는 잔고가 없습니다. " + userId));
    }


}
