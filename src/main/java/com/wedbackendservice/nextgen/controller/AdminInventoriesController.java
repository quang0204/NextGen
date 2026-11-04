package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.inventory.InventoryAdjustmentRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.inventory.*;
import com.wedbackendservice.nextgen.service.InventoriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/inventories")
@RequiredArgsConstructor
public class AdminInventoriesController {

    private final InventoriesService inventoriesService;

    /**
     * API 5.1: [GET] /api/v1/admin/inventories — Bảng theo dõi tồn kho biến thể SKU (STAFF / ADMIN)
     */
    @GetMapping
    public ApiResponse<AdminInventoryListResponse> getInventories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "false") Boolean lowStockOnly) {

        AdminInventoryListResponse responseData = inventoriesService.getAdminInventories(page, size, keyword, status, lowStockOnly);

        return ApiResponse.<AdminInventoryListResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy danh sách tồn kho biến thể thành công")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 5.3: [POST] /api/v1/admin/inventories/adjustments — Lập phiếu kiểm kê điều chỉnh tồn kho MB05 (STAFF / ADMIN)
     * (Đặt trước path variable để tránh xung đột routing)
     */
    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryAdjustmentResponse> adjustInventory(
            @Valid @RequestBody InventoryAdjustmentRequest request) {

        InventoryAdjustmentResponse responseData = inventoriesService.adjustInventory(request);

        return ApiResponse.<InventoryAdjustmentResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Lập phiếu kiểm kê MB05 và cân đối tồn kho thành công!")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 5.4: [GET] /api/v1/admin/inventories/transactions — Sổ cái biến động kho bất biến (STAFF / ADMIN)
     */
    @GetMapping("/transactions")
    public ApiResponse<AdminInventoryTransactionListResponse> getInventoryTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String formCode,
            @RequestParam(required = false) String txnType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        AdminInventoryTransactionListResponse responseData = inventoriesService.getInventoryTransactions(
                page, size, formCode, txnType, startDate, endDate);

        return ApiResponse.<AdminInventoryTransactionListResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Truy vấn sổ cái biến động kho thành công")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 5.5: [GET] /api/v1/admin/inventories/low-stock — Cảnh báo biến thể chạm ngưỡng an toàn (STAFF / ADMIN)
     */
    @GetMapping("/low-stock")
    public ApiResponse<LowStockAlertResponse> getLowStockAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        LowStockAlertResponse responseData = inventoriesService.getLowStockAlerts(page, size);

        return ApiResponse.<LowStockAlertResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy danh sách cảnh báo tồn kho thấp thành công")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 5.2: [GET] /api/v1/admin/inventories/{varId} — Chi tiết tồn kho biến thể SKU cụ thể (STAFF / ADMIN)
     * (Chỉ nhận số nguyên bằng regex :[0-9]+ để tuyệt đối không nuốt các path tĩnh)
     */
    @GetMapping("/{varId:[0-9]+}")
    public ApiResponse<AdminInventoryDetailResponse> getInventoryDetail(@PathVariable Long varId) {
        AdminInventoryDetailResponse responseData = inventoriesService.getInventoryDetail(varId);

        return ApiResponse.<AdminInventoryDetailResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy chi tiết tồn kho SKU thành công")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }
}
