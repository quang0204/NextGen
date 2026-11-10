package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.portal.request.returnrequest.InspectReturnRequest;
import com.wedbackendservice.nextgen.dto.portal.response.returnrequest.AdminReturnListResponse;
import com.wedbackendservice.nextgen.dto.portal.response.returnrequest.InspectReturnResponse;
import com.wedbackendservice.nextgen.service.shared.ReturnsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/returns")
@RequiredArgsConstructor
public class AdminReturnsController {

    private final ReturnsService returnsService;

    /**
     * API 11.2: [GET] /api/v1/admin/returns — Quản lý danh sách yêu cầu đổi trả (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping
    public ApiResponse<AdminReturnListResponse> getReturns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String orderCode,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        int effectiveLimit = size != null ? size : limit;
        return ApiResponse.ok(
                "Lấy danh sách yêu cầu đổi trả thành công!",
                returnsService.getAdminReturns(page, effectiveLimit, status, orderCode, sortBy, sortDir)
        );
    }

    /**
     * API 11.3: [PUT] /api/v1/admin/returns/{id}/inspect — Biên bản kiểm định ngoại quan & Hoàn kho MB04 (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PutMapping("/{id:[0-9]+}/inspect")
    public ApiResponse<InspectReturnResponse> inspectReturn(
            @PathVariable Long id,
            @Valid @RequestBody InspectReturnRequest request) {
        InspectReturnResponse response = returnsService.inspectReturn(id, request);
        String message = "APPROVED".equalsIgnoreCase(response.getStatus())
                ? "Đã hoàn tất giám định, lập biên bản MB04 nhập hoàn kho và phê duyệt hoàn tiền thành công!"
                : ("INSPECTING".equalsIgnoreCase(response.getStatus())
                        ? "Đã ghi nhận biên bản thẩm định ngoại quan thành công!"
                        : "Đã từ chối yêu cầu đổi trả và lập biên bản kiểm định!");
        return ApiResponse.ok(message, response);
    }
}
