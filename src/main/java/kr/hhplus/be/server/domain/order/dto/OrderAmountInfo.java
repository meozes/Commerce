package kr.hhplus.be.server.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderAmountInfo {
    private Integer originalAmount;  // 원래 금액
    private Integer discountAmount;  // 할인 금액
    private Integer finalAmount;     // 최종 금액 (원래 금액 - 할인 금액)

    public static OrderAmountInfo of(Integer originalAmount, Integer discountAmount) {
        return OrderAmountInfo.builder()
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(originalAmount - discountAmount)
                .build();
    }
}
