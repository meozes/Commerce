package kr.hhplus.be.server.domain.order.service;

import kr.hhplus.be.server.domain.coupon.entity.IssuedCoupon;
import kr.hhplus.be.server.domain.order.dto.OrderAmountInfo;
import kr.hhplus.be.server.domain.order.dto.OrderItemCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderAmountCalculator {
    public OrderAmountInfo calculate(List<OrderItemCommand> orderItems, IssuedCoupon issuedCoupon) {
        Integer discountAmount = 0;
        if (issuedCoupon != null) {
            discountAmount = issuedCoupon.getCoupon().getDiscountAmount();
        }

        Integer originalAmount = orderItems.stream()
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
