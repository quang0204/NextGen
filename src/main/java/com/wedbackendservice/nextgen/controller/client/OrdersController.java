package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.client.request.order.CancelOrderRequest;
import com.wedbackendservice.nextgen.dto.client.request.order.CreateOrderRequest;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.CancelOrderResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.CreateOrderResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.MyOrderItemResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.OrderDetailResponse;
import com.wedbackendservice.nextgen.service.shared.OrdersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
        CreateOrderResponse data = ordersService.createOrder(request, idempotencyKey);
        return ApiResponse.created("Đơn hàng #" + data.getOrderCode() + " đã được khởi tạo thành công! Tồn kho đã được giữ chỗ an toàn.", data);
    }

    /**
     * API 8.2: [GET] /api/v1/orders/my-orders — Khách hàng tra cứu lịch sử mua hàng (Authenticated)
     */
    @GetMapping("/my-orders")
    public ApiResponse<PageResponse<MyOrderItemResponse>> getMyOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        return ApiResponse.ok("Lấy danh sách đơn hàng cá nhân thành công", ordersService.getMyOrders(page, size, status));
    }

    /**
     * API 8.3: [GET] /api/v1/orders/{orderCode} — Chi tiết đơn hàng theo mã #DH-XXXX (Authenticated)
     */
    @GetMapping("/{orderCode}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(@PathVariable String orderCode) {
        return ApiResponse.ok("Truy vấn thông tin chi tiết đơn hàng thành công", ordersService.getOrderDetail(orderCode));
    }

    /**
     * API 8.4: [POST] /api/v1/orders/{orderCode}/cancel — Khách hàng hủy đơn & Giải phóng kho (Authenticated)
     */
    @PostMapping("/{orderCode}/cancel")
    public ApiResponse<CancelOrderResponse> cancelOrder(
            @PathVariable String orderCode,
            @Valid @RequestBody CancelOrderRequest request) {
        return ApiResponse.ok("Đơn hàng đã được hủy thành công! Tồn kho giữ chỗ đã được giải phóng hoàn toàn.",
                ordersService.cancelOrder(orderCode, request));
    }
}
