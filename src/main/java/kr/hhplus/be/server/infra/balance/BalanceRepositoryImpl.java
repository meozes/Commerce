package kr.hhplus.be.server.infra.balance;

import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface BalanceRepositoryImpl extends JpaRepository<Balance, Long>, BalanceRepository {
    @Override
    default Optional<Balance> getBalance(Long userId) {
        return findByUserId(userId);
    }

    Optional<Balance> findByUserId(Long userId);
}
