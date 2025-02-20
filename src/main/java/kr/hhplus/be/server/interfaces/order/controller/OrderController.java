package kr.hhplus.be.server.interfaces.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.application.order.OrderFacade;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderInfo;
import kr.hhplus.be.server.domain.order.dto.UserOrderInfo;
import kr.hhplus.be.server.domain.order.usecase.OrderFindService;
import kr.hhplus.be.server.interfaces.common.response.ApiResponse;
import kr.hhplus.be.server.interfaces.order.request.OrderRequest;
import kr.hhplus.be.server.interfaces.order.response.OrderResponse;
import kr.hhplus.be.server.interfaces.order.response.UserOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@Tag(name = "주문 API", description = "주문 생성 API")
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;
    private final OrderFindService orderFindService;

    /**
     * 주문 생성하기
     */
    @Operation(summary = "주문 생성", description = "주문 요청합니다.")
    @PostMapping("/orders")
    public ApiResponse<OrderResponse> createOrder(
            @Parameter(description = "주문 요청 정보")
            @Valid @RequestBody OrderRequest request
    ) {
        OrderCommand command = OrderCommand.from(request);
        OrderInfo info = orderFacade.createOrder(command);
        return ApiResponse.ok(OrderResponse.of(info.getOrder(), info.getOrderItems()));
    }

    /**
     * 유저의 주문 조회하기
     */
    @Operation(summary = "주문 조회", description = "유저의 주문을 조회합니다.")
    @GetMapping("/orders/{userId}")
    public ApiResponse<UserOrderResponse> getOrders(
            @Parameter(name = "userId", description = "사용자 ID", required = true)
            @PathVariable("userId") Long userId
    ) {
        UserOrderInfo info = orderFindService.getUserLatestOrder(userId);
        return ApiResponse.ok(UserOrderResponse.from(info));
    }
}
