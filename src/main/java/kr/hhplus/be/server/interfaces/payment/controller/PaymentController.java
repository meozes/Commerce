package kr.hhplus.be.server.interfaces.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderStatusType;
import kr.hhplus.be.server.domain.payment.dto.PaymentCommand;
import kr.hhplus.be.server.domain.payment.dto.PaymentInfo;
import kr.hhplus.be.server.domain.payment.entity.Payment;
import kr.hhplus.be.server.domain.payment.entity.PaymentStatusType;
import kr.hhplus.be.server.domain.payment.usecase.PaymentService;
import kr.hhplus.be.server.interfaces.common.ApiResponse;
import kr.hhplus.be.server.interfaces.payment.request.PaymentRequest;
import kr.hhplus.be.server.interfaces.payment.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@Tag(name = "결제 API", description = "결제 생성 API")
@RestController
@RequestMapping("api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 생성", description = "결제 요청합니다.")
    @PostMapping()
    public ApiResponse<PaymentResponse> createPayment(
            @Parameter(description = "결제 요청 정보")
            @Valid @RequestBody PaymentRequest request
    ){
        Order order = Order.builder()
                .id(1L)
                .userId(request.getUserId())
                .originalAmount(50000)
                .discountAmount(5000)
                .finalAmount(45000)
                .orderStatus(OrderStatusType.PENDING)
                .build();

        PaymentCommand command = PaymentCommand.from(request);
        PaymentInfo paymentInfo = paymentService.createPayment(command);
        return ApiResponse.ok(PaymentResponse.of(paymentInfo));

    }
}
