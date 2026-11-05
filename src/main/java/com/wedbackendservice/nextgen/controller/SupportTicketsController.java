package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.request.ticket.SupportTicketCreateRequest;
import com.wedbackendservice.nextgen.dto.response.ticket.SupportTicketResponse;
import com.wedbackendservice.nextgen.service.SupportTicketsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support-tickets")
@RequiredArgsConstructor
public class SupportTicketsController {

    private final SupportTicketsService supportTicketsService;

    /**
     * API 12.9: Khách hàng tạo ticket hỗ trợ.
     */
    @PostMapping
    public ApiResponse<SupportTicketResponse> createTicket(
            @Valid @RequestBody SupportTicketCreateRequest request) {
        return ApiResponse.created("Tạo ticket hỗ trợ thành công",
                supportTicketsService.createTicket(request));
    }

    /**
     * API 12.10: Danh sách ticket của tôi.
     */
    @GetMapping("/me")
    public ApiResponse<List<SupportTicketResponse>> getMyTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(supportTicketsService.getMyTickets(page, size));
    }

    /**
     * API 12.11: Xem chi tiết ticket.
     */
    @GetMapping("/{ticketId}")
    public ApiResponse<SupportTicketResponse> getTicket(@PathVariable Long ticketId) {
        return ApiResponse.ok(supportTicketsService.getTicketDetail(ticketId));
    }
}
