package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.portal.request.order.ConfirmOrderRequest;
import com.wedbackendservice.nextgen.dto.portal.request.order.PackExportOrderRequest;
import com.wedbackendservice.nextgen.dto.portal.response.order.AdminOrderListResponse;
import com.wedbackendservice.nextgen.dto.portal.response.order.ConfirmOrderResponse;
import com.wedbackendservice.nextgen.dto.portal.response.order.PackExportOrderResponse;
import com.wedbackendservice.nextgen.service.shared.OrdersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrdersController {

  private final OrdersService ordersService;

  /**
   * API 8.5: [GET] /api/v1/admin/orders — Quản trị toàn bộ danh sách đơn hàng (7 bước) (STAFF /
   * ADMIN)
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @GetMapping
  public ApiResponse<AdminOrderListResponse> getAdminOrders(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false, defaultValue = "ALL") String status,
      @RequestParam(required = false, defaultValue = "ALL") String paymentStatus) {
    return ApiResponse.ok(
        "Lấy danh sách đơn hàng quản trị thành công",
        ordersService.getAdminOrders(page, size, status, paymentStatus));
  }

  /**
   * API 8.6: [POST] /api/v1/admin/orders/{id}/confirm — Xác nhận đơn MB02 & Tự động tạo Vận đơn
   * (STAFF / ADMIN)
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/confirm")
  public ApiResponse<ConfirmOrderResponse> confirmOrder(
      @PathVariable Long id, @Valid @RequestBody ConfirmOrderRequest request) {
    return ApiResponse.ok(
        "Đã duyệt xác nhận đơn hàng MB02 và tự động tạo vận đơn giao hàng!",
        ordersService.confirmOrder(id, request));
  }

  /**
   * API: [POST] /api/v1/admin/orders/{id}/prepare — Chuyển trạng thái đơn hàng sang Đang chuẩn bị
   * hàng (PREPARING) (STAFF / ADMIN)
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/prepare")
  public ApiResponse<com.wedbackendservice.nextgen.dto.client.response.order.OrderDetailResponse>
      prepareOrder(@PathVariable Long id) {
    return ApiResponse.ok(
        "Đã chuyển đơn hàng sang trạng thái Đang chuẩn bị hàng và đẩy sang Vận đơn!",
        ordersService.prepareOrder(id));
  }

  /**
   * API 8.7: [POST] /api/v1/admin/orders/{id}/pack-export — Đóng gói & Xuất kho vật lý MB03 (STAFF
   * / ADMIN)
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/pack-export")
  public ApiResponse<PackExportOrderResponse> packAndExportOrder(
      @PathVariable Long id, @Valid @RequestBody PackExportOrderRequest request) {
    return ApiResponse.ok(
        "Đã hoàn tất đóng gói, lập phiếu xuất kho MB03 và bàn giao đơn vị vận chuyển thành công!",
        ordersService.packAndExportOrder(id, request));
  }

  /**
   * API 8.8: [POST] /api/v1/admin/orders/{id}/confirm-payment — Xác nhận đã nhận tiền chuyển khoản
   * (STAFF / ADMIN)
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/confirm-payment")
  public ApiResponse<com.wedbackendservice.nextgen.dto.client.response.order.OrderDetailResponse>
      confirmPayment(@PathVariable Long id, @RequestParam(required = false) String note) {
    return ApiResponse.ok(
        "Đã xác nhận nhận tiền chuyển khoản cho đơn hàng thành công!",
        ordersService.confirmOrderPayment(id, note));
  }

  /**
   * API 8.9: [POST] /api/v1/admin/orders/{id}/receive-return — Thủ kho nhận hàng hoàn kho & Hoàn
   * tất hủy đơn (STAFF / ADMIN)
   */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/receive-return")
  public ApiResponse<com.wedbackendservice.nextgen.dto.client.response.order.OrderDetailResponse>
      receiveReturnOrder(@PathVariable Long id, @RequestParam(required = false) String note) {
    return ApiResponse.ok(
        "Đã tiếp nhận kiện hàng hoàn về kho, hoàn kho thành công và cập nhật Đã hủy đơn!",
        ordersService.receiveReturnOrder(id, note));
  }
}
