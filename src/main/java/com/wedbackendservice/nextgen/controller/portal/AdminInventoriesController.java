package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.portal.request.inventory.InventoryAdjustmentRequest;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
import com.wedbackendservice.nextgen.dto.portal.response.inventory.*;
import com.wedbackendservice.nextgen.service.portal.InventoriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/inventories")
@RequiredArgsConstructor
public class AdminInventoriesController {

    private final InventoriesService inventoriesService;

    /**
     * API 5.1: [GET] /api/v1/admin/inventories — Bảng theo dõi tồn kho biến thể SKU (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping
    public ApiResponse<AdminInventoryListResponse> getAdminInventories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "false") Boolean lowStockOnly) {
        return ApiResponse.ok("Lấy danh sách tồn kho biến thể thành công",
                inventoriesService.getAdminInventories(page, size, keyword, status, lowStockOnly));
    }

    /**
     * API 5.3: [POST] /api/v1/admin/inventories/adjustments — Lập phiếu kiểm kê điều chỉnh tồn kho MB05 (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryAdjustmentResponse> adjustInventory(
            @Valid @RequestBody InventoryAdjustmentRequest request) {
        return ApiResponse.created("Lập phiếu kiểm kê MB05 và cân đối tồn kho thành công!",
                inventoriesService.adjustInventory(request));
    }

    /**
     * API 5.4: [GET] /api/v1/admin/inventories/transactions — Sổ cái biến động kho bất biến (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping("/transactions")
    public ApiResponse<PageResponse<InventoryTransactionItemResponse>> getInventoryTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String formCode,
            @RequestParam(required = false) String txnType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok("Truy vấn sổ cái biến động kho thành công",
                inventoriesService.getInventoryTransactions(page, size, formCode, txnType, startDate, endDate));
    }

    /**
     * API 5.5: [GET] /api/v1/admin/inventories/low-stock — Cảnh báo biến thể chạm ngưỡng an toàn (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping("/low-stock")
    public ApiResponse<PageResponse<LowStockItemResponse>> getLowStockAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy danh sách cảnh báo tồn kho thấp thành công",
                inventoriesService.getLowStockAlerts(page, size));
    }

    /**
     * API 5.2: [GET] /api/v1/admin/inventories/{varId} — Chi tiết tồn kho biến thể SKU cụ thể (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping("/{varId:[0-9]+}")
    public ApiResponse<AdminInventoryDetailResponse> getInventoryDetail(@PathVariable Long varId) {
        return ApiResponse.ok("Lấy chi tiết tồn kho SKU thành công", inventoriesService.getInventoryDetail(varId));
    }
}
