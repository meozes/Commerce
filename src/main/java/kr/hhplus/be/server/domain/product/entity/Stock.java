package kr.hhplus.be.server.domain.product.entity;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.common.entity.BaseTimeEntity;
import kr.hhplus.be.server.domain.product.exception.InsufficientStockException;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseTimeEntity { // 비관적 락

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
            throw new InsufficientStockException("재고가 부족합니다.");
        }
        this.remainingStock = remainingQuantity;
    }
}
