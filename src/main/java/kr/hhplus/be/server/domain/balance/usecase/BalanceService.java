package kr.hhplus.be.server.domain.balance.usecase;

import kr.hhplus.be.server.common.aop.annotation.Monitored;
import kr.hhplus.be.server.common.aop.annotation.Monitoring;
import kr.hhplus.be.server.domain.balance.validation.AmountValidator;
import kr.hhplus.be.server.domain.balance.validation.UserIdValidator;
import lombok.extern.slf4j.Slf4j;
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


@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository historyRepository;
    private final UserIdValidator userIdValidator;
    private final AmountValidator amountValidator;

    /**
     * 잔고 조회하기
     */
    public BalanceInfo getBalance(BalanceQuery balanceQuery) {
        userIdValidator.validate(balanceQuery.getUserId());
        Balance balance = balanceRepository.getBalance(balanceQuery.getUserId())
                .orElseGet(() -> createBalance(balanceQuery.getUserId()));
        return BalanceInfo.from(balance);
    }

    /**
     * 잔액 충전하기
     */
    @Monitored
    @Monitoring
    @Transactional
    public BalanceInfo chargeBalance(ChargeCommand command) {


        log.info("[잔액 충전 시작] userId={}, chargeAmount={}", command.getUserId(), command.getChargeAmount());

        userIdValidator.validate(command.getUserId());
        amountValidator.validateChargeAmount(command.getChargeAmount());

        Balance balance = balanceRepository.getBalanceWithLock(command.getUserId())
                .orElseGet(() -> createBalance(command.getUserId()));

        Integer beforeBalance = balance.getBalance();
        balance.charge(command.getChargeAmount());
        Balance charged = balanceRepository.save(balance);

        historyRepository.saveHistory(
                beforeBalance,
                charged.getBalance(),
                charged.getId(),
                TransactionType.CHARGE,
                command.getChargeAmount()
        );

        log.info("[잔액 충전 완료] userId={}, beforeBalance={}, afterBalance={}, chargeAmount={}",
                command.getUserId(),
                beforeBalance,
                charged.getBalance(),
                command.getChargeAmount());
        return BalanceInfo.from(charged);
    }

    /**
     * 잔고 차감하기
     */
    @Monitored
    @Monitoring
    @Transactional
    public BalanceInfo deductBalance(Long userId, Integer amount) {

        log.info("[잔고 차감 시작] userId={}, deductAmount={}", userId, amount);

        userIdValidator.validate(userId);
        amountValidator.validateDeductAmount(amount);

        Balance balance = balanceRepository.getBalanceWithLock(userId)
                .orElseGet(() -> createBalance(userId));

        Integer beforeBalance = balance.getBalance();
        balance.deduct(amount);
        Balance deducted = balanceRepository.save(balance);

        historyRepository.saveHistory(
                beforeBalance,
                deducted.getBalance(),
                deducted.getId(),
                TransactionType.USE,
                amount
        );

        log.info("[잔고 차감 완료] userId={}, beforeBalance={}, afterBalance={}, deductedAmount={}",
                userId,
                beforeBalance,
                deducted.getBalance(),
                amount);
        return BalanceInfo.from(balance);
    }

    /**
     * 계좌 만들기
     */
    @Transactional
    public Balance createBalance(Long userId) {
        Balance balance = Balance.builder()
                .userId(userId)
                .balance(0)
                .build();
        return balanceRepository.save(balance);
    }
}
