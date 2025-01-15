package kr.hhplus.be.server.interfaces.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // User
    USER_ID_REQUIRED("유저 ID는 필수입니다."),
    INVALID_USER_ID("유효하지 않은 유저 ID 입니다."),

    // Balance
    CHARGE_AMOUNT_REQUIRED("충전 금액은 필수입니다."),
    INVALID_AMOUNT_INPUT("충전 금액은 100원 이상이어야 합니다."),
    INSUFFICIENT_BALANCE("잔액이 부족합니다."),
    DEDUCTION_AMOUNT_REQUIRED("차감 금액은 필수입니다."),
    INVALID_DEDUCTION_AMOUNT("차감 금액은 0보다 커야 합니다."),

    // Coupon
    COUPON_OUT_OF_STOCK("쿠폰이 모두 소진되었습니다."),
    COUPON_ALREADY_USED("이미 사용된 쿠폰입니다."),
    COUPON_EXPIRED("만료된 쿠폰입니다."),
    COUPON_NOT_FOUND("해당 쿠폰을 발급받은 내역이 없습니다."),
    COUPON_ALREADY_ISSUED("이미 발급받은 쿠폰입니다."),
    INVALID_COUPON("존재하지 않는 쿠폰입니다."),

    // Product, Stock
    INVALID_PRODUCT_ID("유효하지 않은 상품 ID 입니다."),
    PRODUCT_NOT_FOUND("상품이 존재하지 않습니다."),
    PRODUCT_STOCK_NOT_FOUND("상품의 재고 정보가 없습니다."),
    INSUFFICIENT_STOCK("상품의 재고가 부족합니다."),

    // Order
    ORDER_NOT_FOUND("주문이 존재하지 않습니다."),
    ORDER_ITEMS_REQUIRED("주문 상품 목록은 필수입니다."),
    ORDER_ALREADY_COMPLETED("주문이 취소되었거나 처리 완료되었습니다."),
    ORDER_SYNC_FAILED("주문 정보 전송 실패"),

    // Payment
    INVALID_PAYMENT_AMOUNT("결제 금액은 0보다 커야 합니다."),
    PAYMENT_AMOUNT_MISMATCH("결제 금액이 주문서와 불일치 합니다.");


    private final String message;
}
