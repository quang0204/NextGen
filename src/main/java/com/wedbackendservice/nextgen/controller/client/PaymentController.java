package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.client.request.payment.CreatePaymentIntentRequest;
import com.wedbackendservice.nextgen.dto.client.request.payment.SimulateCallbackRequest;
import com.wedbackendservice.nextgen.dto.client.response.payment.PaymentIntentResponse;
import com.wedbackendservice.nextgen.dto.client.response.payment.SimulateCallbackResponse;
import com.wedbackendservice.nextgen.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * API 10.1: Tạo thanh toán QR mô phỏng hoặc COD.
     */
    @PostMapping("/create-intent")
    public ApiResponse<PaymentIntentResponse> createIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        return ApiResponse.ok("Tạo yêu cầu thanh toán thành công",
                paymentService.createPaymentIntent(request));
    }

    /**
     * API 10.2: Xác nhận chuyển khoản mô phỏng.
     */
    @PostMapping("/simulate-callback")
    public ApiResponse<SimulateCallbackResponse> simulateCallback(
            @Valid @RequestBody SimulateCallbackRequest request) {
        return ApiResponse.ok("Xử lý callback thanh toán thành công",
                paymentService.simulateCallback(request));
    }
}
