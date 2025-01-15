package kr.hhplus.be.server.domain.balance.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BalanceRepository {
    Optional<Balance> getBalance(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "javax.persistence.lock.timeout", value = "3000"),
            @QueryHint(name = "javax.persistence.query.timeout", value = "3000")
    })
    @Query("select b from Balance b where b.userId = :userId")
    Optional<Balance> getBalanceWithLock(@Param("userId") Long userId);

    Balance save(Balance balance);

    Optional<Balance> findById(Long id);
}
