package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.order.CancelOrderRequest;
import com.wedbackendservice.nextgen.dto.request.order.CreateOrderRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.order.CancelOrderResponse;
import com.wedbackendservice.nextgen.dto.response.order.CreateOrderResponse;
import com.wedbackendservice.nextgen.dto.response.order.MyOrdersListResponse;
import com.wedbackendservice.nextgen.dto.response.order.OrderDetailResponse;
import com.wedbackendservice.nextgen.service.OrdersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService ordersService;

    /**
     * API 8.1: [POST] /api/v1/orders — Đặt hàng Checkout (Tạo MB01 & Giữ chỗ kho) (Authenticated)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateOrderResponse> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        CreateOrderResponse responseData = ordersService.createOrder(request, idempotencyKey);

        return ApiResponse.<CreateOrderResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Đơn hàng #" + responseData.getOrderCode() + " đã được khởi tạo thành công! Tồn kho đã được giữ chỗ an toàn.")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 8.2: [GET] /api/v1/orders/my-orders — Khách hàng tra cứu lịch sử mua hàng (Authenticated)
     * (Đặt trước path variable {orderCode} để tránh conflict routing)
     */
    @GetMapping("/my-orders")
    public ApiResponse<MyOrdersListResponse> getMyOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false, defaultValue = "ALL") String status) {

        MyOrdersListResponse responseData = ordersService.getMyOrders(page, size, status);

        return ApiResponse.<MyOrdersListResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy danh sách đơn hàng cá nhân thành công")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 8.3: [GET] /api/v1/orders/{orderCode} — Chi tiết đơn hàng theo mã #DH-XXXX (Authenticated)
     */
    @GetMapping("/{orderCode}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(@PathVariable String orderCode) {
        OrderDetailResponse responseData = ordersService.getOrderDetail(orderCode);

        return ApiResponse.<OrderDetailResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Truy vấn thông tin chi tiết đơn hàng thành công")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 8.4: [POST] /api/v1/orders/{orderCode}/cancel — Khách hàng hủy đơn & Giải phóng kho (Authenticated)
     */
    @PostMapping("/{orderCode}/cancel")
    public ApiResponse<CancelOrderResponse> cancelOrder(
            @PathVariable String orderCode,
            @Valid @RequestBody CancelOrderRequest request) {

        CancelOrderResponse responseData = ordersService.cancelOrder(orderCode, request);

        return ApiResponse.<CancelOrderResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Đơn hàng đã được hủy thành công! Tồn kho giữ chỗ đã được giải phóng hoàn toàn.")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }
}
