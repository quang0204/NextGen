package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.client.request.ticket.SupportTicketCreateRequest;
import com.wedbackendservice.nextgen.dto.client.response.ticket.SupportTicketResponse;
import com.wedbackendservice.nextgen.service.client.SupportTicketsService;
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
     * API: Lấy danh sách phiếu hỗ trợ của khách hàng đăng nhập hiện tại theo userId.
     */
    @GetMapping
    public ApiResponse<List<SupportTicketResponse>> getMyTickets(
            @RequestParam(required = false) Long userId) {
        return ApiResponse.ok("Lấy danh sách phiếu hỗ trợ thành công",
                supportTicketsService.getMyTickets(userId));
    }

    /**
     * API 12.4: Gửi phiếu hỗ trợ.
     */
    @PostMapping
    public ApiResponse<SupportTicketResponse> createTicket(
            @Valid @RequestBody SupportTicketCreateRequest request) {
        return ApiResponse.created("Gửi phiếu hỗ trợ thành công",
                supportTicketsService.createTicket(request));
    }
}
