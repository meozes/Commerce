package kr.hhplus.be.server.domain.balance;

import kr.hhplus.be.server.domain.balance.dto.BalanceInfo;
import kr.hhplus.be.server.domain.balance.dto.BalanceQuery;
import kr.hhplus.be.server.domain.balance.dto.ChargeCommand;
import kr.hhplus.be.server.domain.balance.entity.Balance;
import kr.hhplus.be.server.domain.balance.entity.BalanceHistory;
import kr.hhplus.be.server.domain.balance.type.TransactionType;
import kr.hhplus.be.server.domain.balance.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.domain.balance.repository.BalanceRepository;
import kr.hhplus.be.server.domain.balance.usecase.BalanceService;
import kr.hhplus.be.server.domain.balance.validation.AmountValidator;
import kr.hhplus.be.server.domain.balance.validation.UserIdValidator;
import kr.hhplus.be.server.domain.payment.exception.InsufficientBalanceException;
import kr.hhplus.be.server.interfaces.balance.request.ChargeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {
    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceHistoryRepository historyRepository;

    @Mock
    private UserIdValidator userIdValidator;

    @Mock
    private AmountValidator amountValidator;

    @InjectMocks
    private BalanceService balanceService;


    /**
     * 잔고 조회 테스트
     */
    @Test
    @DisplayName("잔고 조회 성공")
    void getBalance_Success() {
        // given
        Long userId = 1L;
        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .balance(1000)
                .build();

        doNothing().when(userIdValidator).validate(userId);
        when(balanceRepository.getBalance(userId)).thenReturn(Optional.ofNullable(balance));

        // when
        BalanceInfo result = balanceService.getBalance(BalanceQuery.of(userId));

        // then
        assertAll(
                () -> assertEquals(userId, result.getUserId()),
                () -> assertEquals(1000, result.getBalance()),
                () -> verify(userIdValidator).validate(userId),
                () -> verify(balanceRepository).getBalance(userId)
        );
    }

    @Test
    @DisplayName("유효하지 않은 userId - 잔고 조회 실패")
    void getBalance_Fail() {
        Long nullUserId = null;
        Long invalidUserId = -1L;

        doThrow(new IllegalArgumentException("유저 ID는 필수입니다."))
                .when(userIdValidator).validate(nullUserId);
        doThrow(new IllegalArgumentException("유효하지 않은 유저 ID 입니다."))
                .when(userIdValidator).validate(invalidUserId);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                balanceService.getBalance(BalanceQuery.of(nullUserId))
        );
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
                balanceService.getBalance(BalanceQuery.of(invalidUserId))
        );

        assertAll(
                () -> assertEquals("유저 ID는 필수입니다.", exception.getMessage()),
                () -> verify(userIdValidator).validate(nullUserId),
                () -> verify(balanceRepository, never()).getBalance(any())
        );
        assertAll(
                () -> assertEquals("유효하지 않은 유저 ID 입니다.", exception2.getMessage()),
                () -> verify(userIdValidator).validate(invalidUserId),
                () -> verify(balanceRepository, never()).getBalance(any())
        );
    }


    /**
     * 잔고 충전 테스트
     */
    @Test
    @DisplayName("충전 성공")
    void chargeBalance_Success() {
        // given
        Long userId = 1L;
        Integer chargeAmount = 10000;
        ChargeRequest request = new ChargeRequest(userId, chargeAmount);
        ChargeCommand command = ChargeCommand.from(request);

        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .balance(1000)
                .build();

        Balance chargedBalance = Balance.builder()
                .id(1L)
                .userId(userId)
                .balance(11000)
                .build();

        BalanceHistory history = BalanceHistory.builder()
                .id(1L)
                .balanceId(1L)
                .type(TransactionType.CHARGE)
                .amount(chargeAmount)
                .totalAmount(11000)
                .build();

        // when
        doNothing().when(userIdValidator).validate(userId);
        doNothing().when(amountValidator).validateChargeAmount(chargeAmount);
        when(balanceRepository.getBalance(userId)).thenReturn(Optional.ofNullable(balance));
        when(balanceRepository.save(any(Balance.class))).thenReturn(chargedBalance);
        when(historyRepository.saveHistory(1000, 11000, 1L, TransactionType.CHARGE, chargeAmount))
                .thenReturn(history);

        BalanceInfo result = balanceService.chargeBalance(command);

        // then
        assertAll(
                () -> assertEquals(userId, result.getUserId()),
                () -> assertEquals(11000, result.getBalance()),
                () -> verify(userIdValidator).validate(userId),
                () -> verify(amountValidator).validateChargeAmount(chargeAmount),
                () -> verify(balanceRepository).getBalance(userId),
                () -> verify(historyRepository).saveHistory(1000, 11000, 1L, TransactionType.CHARGE, chargeAmount)
        );
    }


    @Test
    @DisplayName("잔고가 없는 경우 - 새로운 계좌 생성 후 충전 성공")
    void chargeBalance_WithNoBalance_Success() {
        // given
        Long userId = 1L;
        Integer chargeAmount = 10000;
        ChargeRequest request = new ChargeRequest(userId, chargeAmount);
        ChargeCommand command = ChargeCommand.from(request);

        Balance newBalance = Balance.builder()
                .id(1L)
                .userId(userId)
                .balance(0)      // 새로 생성된 계좌의 초기 잔액
                .build();

        Balance chargedBalance = Balance.builder()
                .id(1L)
                .userId(userId)
                .balance(10000)  // 충전 후 잔액
                .build();


        // when
        doNothing().when(userIdValidator).validate(userId);
        doNothing().when(amountValidator).validateChargeAmount(chargeAmount);
        when(balanceRepository.getBalance(userId)).thenReturn(Optional.empty());
        when(balanceRepository.save(any(Balance.class))).thenReturn(newBalance, chargedBalance);
        when(historyRepository.saveHistory(0, 10000, 1L, TransactionType.CHARGE, chargeAmount))
                .thenReturn(any(BalanceHistory.class));

        BalanceInfo result = balanceService.chargeBalance(command);

        // then
        assertAll(
                () -> assertEquals(userId, result.getUserId()),
                () -> assertEquals(10000, result.getBalance()),
                () -> verify(balanceRepository).getBalance(userId),
                () -> verify(balanceRepository, times(2)).save(any(Balance.class)),
                () -> verify(historyRepository).saveHistory(0, 10000, 1L, TransactionType.CHARGE, chargeAmount)
        );
    }

    @Test
    @DisplayName("유효하지 않은 금액 충전 시도 - 충전 실패")
    void chargeBalance_NegativeAmount_Fail() {
        // given
        Long userId = 1L;
        Integer invalidAmount = -1000;
        ChargeRequest request = new ChargeRequest(userId, invalidAmount);
        ChargeCommand command = ChargeCommand.from(request);

        doNothing().when(userIdValidator).validate(userId);
        doThrow(new IllegalArgumentException("충전 금액은 100원 보다 커야 합니다."))
                .when(amountValidator).validateChargeAmount(invalidAmount);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                balanceService.chargeBalance(command)
        );

        assertAll(
                () -> assertEquals("충전 금액은 100원 보다 커야 합니다.", exception.getMessage()),
                () -> verify(userIdValidator).validate(userId),
                () -> verify(amountValidator).validateChargeAmount(invalidAmount),
                () -> verify(balanceRepository, never()).getBalance(any()),
                () -> verify(historyRepository, never()).saveHistory(anyInt(), anyInt(), anyLong(), any(), anyInt())
        );
    }

    /**
     * 잔고 차감 테스트
     */
    @Test
    @DisplayName("잔액 차감 성공")
    void deductBalance_Success() {
        // given
        Long userId = 1L;
        Integer deductAmount = 500;

        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .balance(1000)
                .build();

        Balance deductedBalance = Balance.builder()
                .id(1L)
                .userId(userId)
                .balance(500)  // 1000 - 500
                .build();

        // when
        doNothing().when(userIdValidator).validate(userId);
        doNothing().when(amountValidator).validateDeductAmount(deductAmount);
        when(balanceRepository.getBalanceWithLock(userId)).thenReturn(balance);
        when(balanceRepository.save(any(Balance.class))).thenReturn(deductedBalance);
        when(historyRepository.saveHistory(1000, 500, 1L, TransactionType.USE, deductAmount))
                .thenReturn(any(BalanceHistory.class));

        BalanceInfo result = balanceService.deductBalance(userId, deductAmount);

        // then
        assertAll(
                () -> assertEquals(userId, result.getUserId()),
                () -> assertEquals(500, result.getBalance()),
                () -> verify(userIdValidator).validate(userId),
                () -> verify(amountValidator).validateDeductAmount(deductAmount),
                () -> verify(balanceRepository).getBalanceWithLock(userId),
                () -> verify(balanceRepository).save(any(Balance.class)),
                () -> verify(historyRepository).saveHistory(1000, 500, 1L, TransactionType.USE, deductAmount)
        );
    }

    @Test
    @DisplayName("잔액이 없는 경우 - 새로운 계좌 생성 후 차감 실패")
    void deductBalance_WithNoBalance_Fail() {
        // given
        Long userId = 1L;
        Integer deductAmount = 500;

        Balance newBalance = Balance.builder()
                .id(1L)
                .userId(userId)
                .balance(0)
                .build();

        // when
        doNothing().when(userIdValidator).validate(userId);
        doNothing().when(amountValidator).validateDeductAmount(deductAmount);
        when(balanceRepository.getBalanceWithLock(userId)).thenReturn(null);
        when(balanceRepository.save(any(Balance.class))).thenReturn(newBalance);

        // then
        assertThrows(InsufficientBalanceException.class, () ->
                balanceService.deductBalance(userId, deductAmount)
        );

        assertAll(
                () -> verify(balanceRepository).getBalanceWithLock(userId),
                () -> verify(balanceRepository).save(any(Balance.class)),
                () -> verify(historyRepository, never()).saveHistory(anyInt(), anyInt(), anyLong(), any(), anyInt())
        );
    }

    @Test
    @DisplayName("잔액 부족 - 차감 실패")
    void deductBalance_NotEnoughBalance_Fail() {
        // given
        Long userId = 1L;
        Integer deductAmount = 2000;

        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .balance(1000)
                .build();

        // when
        doNothing().when(userIdValidator).validate(userId);
        doNothing().when(amountValidator).validateDeductAmount(deductAmount);
        when(balanceRepository.getBalanceWithLock(userId)).thenReturn(balance);

        // then
        assertThrows(InsufficientBalanceException.class, () ->
                balanceService.deductBalance(userId, deductAmount)
        );

        verify(historyRepository, never()).saveHistory(anyInt(), anyInt(), anyLong(), any(), anyInt());
    }
}