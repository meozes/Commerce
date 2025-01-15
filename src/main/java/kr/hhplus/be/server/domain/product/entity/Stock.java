package kr.hhplus.be.server.domain.product.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import kr.hhplus.be.server.interfaces.common.ErrorCode;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer originStock;

    private Integer remainingStock;

    public void deductStock(int quantity) {
        int remainingQuantity = this.remainingStock - quantity;
        if (remainingQuantity < 0) {
            throw new IllegalStateException(ErrorCode.INSUFFICIENT_STOCK.getMessage());
        }
        this.remainingStock = remainingQuantity;
    }

    public void restoreStock(int quantity) {
        this.remainingStock = this.remainingStock + quantity;
    }
}
