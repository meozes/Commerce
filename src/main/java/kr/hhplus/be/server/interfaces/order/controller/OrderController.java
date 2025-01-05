package kr.hhplus.be.server.interfaces.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.entity.OrderStatusType;
import kr.hhplus.be.server.interfaces.common.ApiResponse;
import kr.hhplus.be.server.interfaces.order.request.OrderRequest;
import kr.hhplus.be.server.interfaces.order.response.OrderResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@Tag(name = "주문 API", description = "주문 생성 API")
@RestController
@RequestMapping("api/orders")
public class OrderController {

    @Operation(summary = "주문 생성", description = "주문 요청합니다.")
    @PostMapping()
    public ApiResponse<OrderResponse> createOrder(
            @Parameter(description = "주문 요청 정보")
            @Valid @RequestBody OrderRequest request
    ) {

        Order order = Order.builder()
                .id(1L)
                .userId(request.getUserId())
                .originalAmount(50000)
                .discountAmount(request.getCouponId() != null ? 5000 : 0)  // 쿠폰 있으면 5000원 할인
                .finalAmount(45000)
                .orderStatus(OrderStatusType.PENDING)
                .build();

        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemRequest -> OrderItem.builder()
                        .id((long) (Math.random() * 100))  // 테스트용 랜덤 ID
                        .order(order)
                        .productId(itemRequest.getProductId())
                        .productName("촉촉한 초코칩")
                        .quantity(itemRequest.getQuantity())
                        .productPrice(10000)  // 테스트용 가격
                        .totalPrice((10000 * itemRequest.getQuantity()))
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.ok(OrderResponse.of(order, orderItems));

    }
}
