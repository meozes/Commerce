package kr.hhplus.be.server.domain.balance.usecase;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.balance.exception.ChargeBalanceException;
import kr.hhplus.be.server.domain.balance.validation.AmountValidator;
import kr.hhplus.be.server.domain.balance.validation.UserIdValidator;
import kr.hhplus.be.server.domain.payment.exception.InsufficientBalanceException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository historyRepository;
    private final UserIdValidator userIdValidator;
    private final AmountValidator amountValidator;

    public BalanceInfo getBalance(BalanceQuery balanceQuery) {
        userIdValidator.validate(balanceQuery.getUserId());
        Balance balance = balanceRepository.getBalance(balanceQuery.getUserId())
                .orElseGet(() -> createBalance(balanceQuery.getUserId()));
        return BalanceInfo.from(balance);
    }

    @Transactional
    public BalanceInfo chargeBalance(ChargeCommand command) {
        userIdValidator.validate(command.getUserId());
        amountValidator.validateChargeAmount(command.getChargeAmount());

        Balance balance = balanceRepository.getBalance(command.getUserId())
                .orElseGet(() -> createBalance(command.getUserId()));

        Integer beforeBalance = balance.getBalance();
        balance.charge(command.getChargeAmount());

        try {
            balance = balanceRepository.save(balance);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ChargeBalanceException("충전 처리 중 오류 발생. 다시 시도해주세요.");
        }

        historyRepository.saveHistory(
                beforeBalance,
                balance.getBalance(),
                balance.getId(),
                TransactionType.CHARGE,
                command.getChargeAmount()
        );
        return BalanceInfo.from(balance);
    }

    @Transactional
    public BalanceInfo deductBalance(Long userId, Integer amount) {
        userIdValidator.validate(userId);
        amountValidator.validateDeductAmount(amount);

        Balance balance = balanceRepository.getBalanceWithLock(userId);
        if (balance == null) {
            balance = createBalance(userId);
        }

        Integer beforeBalance = balance.getBalance();
        balance.deduct(amount);
        balance = balanceRepository.save(balance);

        historyRepository.saveHistory(
                beforeBalance,
                balance.getBalance(),
                balance.getId(),
                TransactionType.USE,
                amount
        );
        return BalanceInfo.from(balance);
    }

    @Transactional
    public Balance createBalance(Long userId) {
        Balance balance = Balance.builder()
                .userId(userId)
                .balance(0)
                .build();
        return balanceRepository.save(balance);
    }
}
