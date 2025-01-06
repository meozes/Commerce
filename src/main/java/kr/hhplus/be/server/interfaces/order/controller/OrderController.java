package kr.hhplus.be.server.interfaces.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.entity.OrderStatusType;
import kr.hhplus.be.server.domain.order.usecase.OrderService;
import kr.hhplus.be.server.interfaces.common.ApiResponse;
import kr.hhplus.be.server.interfaces.order.request.OrderRequest;
import kr.hhplus.be.server.interfaces.order.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@Tag(name = "주문 API", description = "주문 생성 API")
@RestController
@RequestMapping("api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "주문 요청합니다.")
    @PostMapping()
    public ApiResponse<OrderResponse> createOrder(
            @Parameter(description = "주문 요청 정보")
            @Valid @RequestBody OrderRequest request
    ) {
        OrderCommand command = OrderCommand.from(request);
        OrderInfo info = orderService.createOrder(command);
        return ApiResponse.ok(OrderResponse.of(info.getOrder(), info.getOrderItems()));
    }
}
