package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.portal.response.admin.AuditLogListResponse;
import com.wedbackendservice.nextgen.service.portal.AdminAuditLogsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogsController {

    private final AdminAuditLogsService adminAuditLogsService;

    /**
     * API 13.5: Nhật ký kiểm toán, chỉ đọc (ADMIN).
     */
    @GetMapping
    public ApiResponse<AuditLogListResponse> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(adminAuditLogsService.getLogs(
                page, size, userId, action, entityName, fromDate, toDate));
    }
}
