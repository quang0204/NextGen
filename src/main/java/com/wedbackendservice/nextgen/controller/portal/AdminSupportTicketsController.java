package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.portal.request.ticket.SupportTicketResolveRequest;
import com.wedbackendservice.nextgen.dto.portal.response.ticket.AdminSupportTicketListResponse;
import com.wedbackendservice.nextgen.dto.client.response.ticket.SupportTicketResponse;
import com.wedbackendservice.nextgen.service.client.SupportTicketsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/support-tickets")
@RequiredArgsConstructor
public class AdminSupportTicketsController {

    private final SupportTicketsService supportTicketsService;

    /**
     * API 12.5: Quản lý phiếu hỗ trợ.
     */
    @GetMapping
    public ApiResponse<AdminSupportTicketListResponse> getTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search) {
        return ApiResponse.ok(
                supportTicketsService.getAdminTickets(page, size, status, priority, search));
    }

    /**
     * API 12.5 phụ: Cập nhật và phản hồi phiếu hỗ trợ.
     */
    @PutMapping("/{id}")
    public ApiResponse<SupportTicketResponse> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody SupportTicketResolveRequest request) {
        return ApiResponse.ok("Cập nhật phiếu hỗ trợ thành công",
                supportTicketsService.updateTicket(id, request));
    }
}
