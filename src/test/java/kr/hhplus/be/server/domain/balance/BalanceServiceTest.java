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
import kr.hhplus.be.server.interfaces.balance.request.ChargeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {
    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceHistoryRepository historyRepository;

    @InjectMocks
    private BalanceService balanceService;

    @Test
    @DisplayName("userId < 0 - 잔고 조회 실패")
    void getBalance_Fail() {
        Long userId = -1L;
        BalanceQuery query = BalanceQuery.of(userId);

        assertThrows(IllegalArgumentException.class, () -> {
            balanceService.getBalance(query);
        });
    }

    @Test
    @DisplayName("userId < 0 - 충전 실패")
    void chargeBalance_invalidUserId_Fail() {
        Long userId = -1L;
        ChargeRequest request = new ChargeRequest(userId, 10000);
        ChargeCommand command = ChargeCommand.from(request);

        assertThrows(IllegalArgumentException.class, () -> {
            balanceService.chargeBalance(command);
        });

    }

    @Test
    @DisplayName("충전 금액 < 100 - 충전 실패")
    void chargeBalance_invalidAmount_Fail() {
        Integer amount = 10;

        ChargeRequest request = new ChargeRequest(1L, amount);
        ChargeCommand command = ChargeCommand.from(request);

        assertThrows(IllegalArgumentException.class, () -> {
            balanceService.chargeBalance(command);
        });

    }

    @Test
    @DisplayName("충전 성공")
    void chargeBalance_Success() {
        ChargeRequest request = new ChargeRequest(1L, 10000);
        ChargeCommand command = ChargeCommand.from(request);

        Balance originalBalance = Balance.builder()
                .id(1L)
                .userId(1L)
                .balance(1000)
                .build();

        Balance chargedBalance = Balance.builder()
                .id(1L)
                .userId(1L)
                .balance(11000) // 기존 1000 + 충전 10000
                .build();

        BalanceHistory history = BalanceHistory.builder()
                .id(1L)
                .balanceId(1L)
                .type(TransactionType.CHARGE)
                .amount(10000)
                .totalAmount(11000)
                .build();


        // when
        when(balanceRepository.getBalanceWithLock(1L)).thenReturn(originalBalance);
        when(balanceRepository.chargeBalance(command)).thenReturn(chargedBalance);
        when(historyRepository.saveHistory(1000, 11000, 1L, TransactionType.CHARGE, 10000))
                .thenReturn(history);

        BalanceInfo result = balanceService.chargeBalance(command);

        // then
        assertAll(
                () -> assertEquals(1L, result.getUserId()),
                () -> assertEquals(11000, result.getBalance()),
                () -> verify(balanceRepository).getBalanceWithLock(1L),
                () -> verify(balanceRepository).chargeBalance(command),
                () -> verify(historyRepository).saveHistory(1000, 11000, 1L, TransactionType.CHARGE, 10000)
        );



    }


}
