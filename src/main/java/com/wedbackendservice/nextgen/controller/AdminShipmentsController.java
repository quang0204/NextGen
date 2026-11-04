package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.shipment.AdminShipmentItemResponse;
import com.wedbackendservice.nextgen.dto.response.shipment.ShipmentFailDeliveryResponse;
import com.wedbackendservice.nextgen.dto.response.shipment.ShipmentStatusSyncResponse;
import com.wedbackendservice.nextgen.dto.request.shipment.ShipmentFailDeliveryRequest;
import com.wedbackendservice.nextgen.dto.request.shipment.ShipmentStatusUpdateRequest;
import com.wedbackendservice.nextgen.service.ShipmentsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/shipments")
@RequiredArgsConstructor
public class AdminShipmentsController {

    private final ShipmentsService shipmentsService;

    /**
     * API 9.2: Quản trị danh sách vận đơn (Admin Portal) (STAFF / ADMIN)
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminShipmentItemResponse>> getAdminShipments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ApiResponse.<PageResponse<AdminShipmentItemResponse>>builder()
                .success(true)
                .statusCode(200)
                .data(shipmentsService.getAdminShipments(page, size, carrier, status, search))
                .build();
    }

    /**
     * API 9.3: Cập nhật trạng thái vận đơn & Tự động đồng bộ 2 chiều (STAFF / ADMIN)
     */
    @PutMapping("/{id}/status")
    public ApiResponse<ShipmentStatusSyncResponse> updateShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateRequest request) {
        return ApiResponse.<ShipmentStatusSyncResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật tiến trình vận đơn thành công. Trạng thái Đơn hàng đã được tự động đồng bộ!")
                .data(shipmentsService.updateShipmentStatus(id, request))
                .build();
    }

    /**
     * API 9.4: Xử lý giao hàng thất bại, tự động hủy đơn và hoàn kho (STAFF / ADMIN)
     */
    @PostMapping("/{id}/fail-delivery")
    public ApiResponse<ShipmentFailDeliveryResponse> failDelivery(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentFailDeliveryRequest request) {
        return ApiResponse.<ShipmentFailDeliveryResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Đã ghi nhận giao hàng thất bại. Đơn hàng đã tự động chuyển sang HỦY và tồn kho đã được hoàn lại đầy đủ!")
                .data(shipmentsService.failDelivery(id, request))
                .build();
    }
}
