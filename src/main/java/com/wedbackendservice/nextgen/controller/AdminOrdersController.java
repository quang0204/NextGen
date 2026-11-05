package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.order.ConfirmOrderRequest;
import com.wedbackendservice.nextgen.dto.request.order.PackExportOrderRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.order.AdminOrderListResponse;
import com.wedbackendservice.nextgen.dto.response.order.ConfirmOrderResponse;
import com.wedbackendservice.nextgen.dto.response.order.PackExportOrderResponse;
import com.wedbackendservice.nextgen.service.OrdersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrdersController {

    private final OrdersService ordersService;

    /**
     * API 8.5: [GET] /api/v1/admin/orders — Quản trị toàn bộ danh sách đơn hàng (7 bước) (STAFF / ADMIN)
     */
    @GetMapping
    public ApiResponse<AdminOrderListResponse> getAdminOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "ALL") String paymentStatus) {
        return ApiResponse.ok("Lấy danh sách đơn hàng quản trị thành công",
                ordersService.getAdminOrders(page, size, status, paymentStatus));
    }

    /**
     * API 8.6: [POST] /api/v1/admin/orders/{id}/confirm — Xác nhận đơn MB02 & Tự động tạo Vận đơn (STAFF / ADMIN)
     */
    @PostMapping("/{id:[0-9]+}/confirm")
    public ApiResponse<ConfirmOrderResponse> confirmOrder(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmOrderRequest request) {
        return ApiResponse.ok("Đã duyệt xác nhận đơn hàng MB02 và tự động tạo vận đơn giao hàng!",
                ordersService.confirmOrder(id, request));
    }

    /**
     * API 8.7: [POST] /api/v1/admin/orders/{id}/pack-export — Đóng gói & Xuất kho vật lý MB03 (STAFF / ADMIN)
     */
    @PostMapping("/{id:[0-9]+}/pack-export")
    public ApiResponse<PackExportOrderResponse> packAndExportOrder(
            @PathVariable Long id,
            @Valid @RequestBody PackExportOrderRequest request) {
        return ApiResponse.ok("Đã hoàn tất đóng gói, lập phiếu xuất kho MB03 và bàn giao đơn vị vận chuyển thành công!",
                ordersService.packAndExportOrder(id, request));
    }
}
