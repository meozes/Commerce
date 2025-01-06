package kr.hhplus.be.server.domain.balance.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.domain.balance.dto.BalanceQuery;
import kr.hhplus.be.server.domain.balance.dto.ChargeCommand;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface BalanceRepository {
    Balance getBalance(Long userId);

    Balance chargeBalance(ChargeCommand command);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Query("select b from Balance b where b.userId = :userId")
    Balance getBalanceWithLock(@Param("userId") Long userId);

    Balance createBalance(Long userId);
}
