package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.request.ticket.SupportTicketResolveRequest;
import com.wedbackendservice.nextgen.dto.response.ticket.AdminSupportTicketListResponse;
import com.wedbackendservice.nextgen.dto.response.ticket.SupportTicketResponse;
import com.wedbackendservice.nextgen.service.SupportTicketsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/support-tickets")
@RequiredArgsConstructor
public class AdminSupportTicketsController {

    private final SupportTicketsService supportTicketsService;

    /**
     * API 12.12: Quản trị danh sách ticket hỗ trợ.
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
     * API 12.13: Xem chi tiết ticket (STAFF / ADMIN).
     */
    @GetMapping("/{ticketId}")
    public ApiResponse<SupportTicketResponse> getTicket(@PathVariable Long ticketId) {
        return ApiResponse.ok(supportTicketsService.getTicketDetail(ticketId));
    }

    /**
     * API 12.14: Xử lý ticket - đổi trạng thái và ghi chú.
     */
    @PutMapping("/{ticketId}/resolve")
    public ApiResponse<SupportTicketResponse> resolveTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody SupportTicketResolveRequest request) {
        return ApiResponse.ok("Cập nhật ticket thành công",
                supportTicketsService.resolveTicket(ticketId, request));
    }
}
