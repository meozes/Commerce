package kr.hhplus.be.server.domain.balance.entity;


import jakarta.persistence.*;
import kr.hhplus.be.server.domain.balance.type.TransactionType;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BalanceHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long balanceId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private Integer amount;

    private Integer totalAmount;
}
