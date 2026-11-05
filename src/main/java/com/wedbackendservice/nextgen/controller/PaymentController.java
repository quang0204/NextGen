package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.request.payment.CreatePaymentIntentRequest;
import com.wedbackendservice.nextgen.dto.request.payment.MomoIpnRequest;
import com.wedbackendservice.nextgen.dto.response.payment.PaymentIntentResponse;
import com.wedbackendservice.nextgen.dto.response.payment.VnpayIpnResponse;
import com.wedbackendservice.nextgen.service.payment.PaymentService;
import com.wedbackendservice.nextgen.util.ClientIpHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * API 10.1: Tạo yêu cầu thanh toán cho đơn hàng (Customer)
     */
    @PostMapping("/intent")
    public ApiResponse<PaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        return ApiResponse.ok("Tạo yêu cầu thanh toán thành công",
                paymentService.createPaymentIntent(request, ClientIpHelper.getClientIp()));
    }

    /**
     * API 10.2: Xem giao dịch thanh toán mới nhất của đơn hàng (Customer / Staff)
     */
    @GetMapping("/order/{orderId}")
    public ApiResponse<PaymentIntentResponse> getPaymentByOrder(@PathVariable Long orderId) {
        return ApiResponse.ok("Lấy thông tin thanh toán thành công",
                paymentService.getByOrderId(orderId));
    }

    /**
     * API 10.3: VNPay redirect về sau khi khách thanh toán (Public)
     */
    @GetMapping("/vnpay/return")
    public ApiResponse<PaymentIntentResponse> vnpayReturn(@RequestParam Map<String, String> params) {
        PaymentIntentResponse data = paymentService.handleVnpayReturn(params);
        return ApiResponse.ok(data.getMessage(), data);
    }

    /**
     * API 10.4: VNPay IPN - VNPay gọi server-to-server (Public)
     */
    @GetMapping("/vnpay/ipn")
    public VnpayIpnResponse vnpayIpn(@RequestParam Map<String, String> params) {
        return paymentService.handleVnpayIpn(params);
    }

    /**
     * API 10.5: MoMo redirect về sau khi khách thanh toán (Public)
     */
    @GetMapping("/momo/return")
    public ApiResponse<PaymentIntentResponse> momoReturn(@RequestParam Map<String, String> params) {
        PaymentIntentResponse data = paymentService.handleMomoReturn(params);
        return ApiResponse.ok(data.getMessage(), data);
    }

    /**
     * API 10.6: MoMo IPN - MoMo gọi server-to-server (Public)
     */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(@Valid @RequestBody MomoIpnRequest request) {
        paymentService.handleMomoIpn(request);
        return ResponseEntity.noContent().build();
    }
}
