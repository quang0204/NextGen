package com.wedbackendservice.nextgen.module.order.controller.portal;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.request.InspectReturnRequest;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.AdminReturnListResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.InspectReturnResponse;
import com.wedbackendservice.nextgen.module.order.service.ReturnsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/returns")
@RequiredArgsConstructor
public class AdminReturnsController {

  private final ReturnsService returnsService;

  /** API 11.2: — Quản lý danh sách yêu cầu đổi trả (STAFF / ADMIN) */
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
        returnsService.getAdminReturns(page, effectiveLimit, status, orderCode, sortBy, sortDir));
  }

  /** API 11.3: — Biên bản kiểm định ngoại quan & Hoàn kho (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PutMapping("/{id:[0-9]+}/inspect")
  public ApiResponse<InspectReturnResponse> inspectReturn(
      @PathVariable Long id, @Valid @RequestBody InspectReturnRequest request) {
    InspectReturnResponse response = returnsService.inspectReturn(id, request);
    String message =
        "APPROVED".equalsIgnoreCase(response.getStatus())
            ? "Đã hoàn tất giám định, lập biên bản nhập hoàn kho và phê duyệt hoàn tiền thành công!"
            : ("INSPECTING".equalsIgnoreCase(response.getStatus())
                ? "Đã ghi nhận biên bản thẩm định ngoại quan thành công!"
                : "Đã từ chối yêu cầu đổi trả và lập biên bản kiểm định!");
    return ApiResponse.ok(message, response);
  }
}
