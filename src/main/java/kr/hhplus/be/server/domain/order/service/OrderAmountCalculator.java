package kr.hhplus.be.server.domain.order.service;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.order.dto.OrderAmountInfo;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderAmountCalculator {
    public OrderAmountInfo calculate(OrderCommand command, IssuedCoupon issuedCoupon) {
        Integer discountAmount = 0;
        if (issuedCoupon != null) {
            discountAmount = issuedCoupon.getCoupon().getDiscountAmount();
        }

        Integer originalAmount = command.getOrderItems().stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();
        Integer finalAmount = originalAmount - discountAmount;

        return OrderAmountInfo.builder()
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }
}
