package com.wedbackendservice.nextgen.module.notification.controller.client;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.notification.dto.client.request.SupportTicketCreateRequest;
import com.wedbackendservice.nextgen.module.notification.dto.client.response.SupportTicketResponse;
import com.wedbackendservice.nextgen.module.notification.service.client.SupportTicketsService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/support-tickets")
@RequiredArgsConstructor
public class SupportTicketsController {

  private final SupportTicketsService supportTicketsService;

  /** API: Lấy danh sách phiếu hỗ trợ của khách hàng đăng nhập hiện tại theo userId. */
  @GetMapping
  public ApiResponse<List<SupportTicketResponse>> getMyTickets(
      @RequestParam(required = false) Long userId) {
    return ApiResponse.ok(
        "Lấy danh sách phiếu hỗ trợ thành công", supportTicketsService.getMyTickets(userId));
  }

  /** API 12.4: Gửi phiếu hỗ trợ. */
  @PostMapping
  public ApiResponse<SupportTicketResponse> createTicket(
      @Valid @RequestBody SupportTicketCreateRequest request) {
    return ApiResponse.created(
        "Gửi phiếu hỗ trợ thành công", supportTicketsService.createTicket(request));
  }
}
