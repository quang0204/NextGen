package com.wedbackendservice.nextgen.module.order.controller.portal;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.request.ConfirmOrderRequest;
import com.wedbackendservice.nextgen.module.order.dto.portal.request.PackExportOrderRequest;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.AdminOrderListResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.ConfirmOrderResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.PackExportOrderResponse;
import com.wedbackendservice.nextgen.module.order.service.OrdersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/admin/orders", "/admin/orders"})
@RequiredArgsConstructor
public class AdminOrdersController {

  private final OrdersService ordersService;

  /** API 8.5:— Quản trị toàn bộ danh sách đơn hàng (STAFF / ADMIN) */
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

  /** API 8.6:— Xác nhận đơn & Tự động tạo Vận đơn (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/confirm")
  public ApiResponse<ConfirmOrderResponse> confirmOrder(
      @PathVariable Long id, @Valid @RequestBody ConfirmOrderRequest request) {
    ConfirmOrderResponse response = ordersService.confirmOrder(id, request);
    String orderCode =
        response != null && response.getOrderCode() != null
            ? response.getOrderCode()
            : String.valueOf(id);
    return ApiResponse.ok(
        String.format("Đã duyệt xác nhận đơn hàng %s và tự động tạo vận đơn giao hàng!", orderCode),
        response);
  }

  /** API: — Chuyển trạng thái đơn hàng sang Đang chuẩn bị hàng (PREPARING) (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/prepare")
  public ApiResponse<
          com.wedbackendservice.nextgen.module.order.dto.client.response.OrderDetailResponse>
      prepareOrder(@PathVariable Long id) {
    return ApiResponse.ok(
        "Đã chuyển đơn hàng sang trạng thái Đang chuẩn bị hàng và đẩy sang Vận đơn!",
        ordersService.prepareOrder(id));
  }

  /** API 8.7: — Đóng gói & Xuất kho vật lý MB03 (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/pack-export")
  public ApiResponse<PackExportOrderResponse> packAndExportOrder(
      @PathVariable Long id, @RequestBody(required = false) PackExportOrderRequest request) {
    return ApiResponse.ok(
        "Đã hoàn tất đóng gói, lập phiếu xuất kho MB03 và bàn giao đơn vị vận chuyển thành công!",
        ordersService.packAndExportOrder(
            id, request != null ? request : new PackExportOrderRequest()));
  }

  /** API 8.8: — Xác nhận đã nhận tiền chuyển khoản (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/confirm-payment")
  public ApiResponse<
          com.wedbackendservice.nextgen.module.order.dto.client.response.OrderDetailResponse>
      confirmPayment(@PathVariable Long id, @RequestParam(required = false) String note) {
    return ApiResponse.ok(
        "Đã xác nhận nhận tiền chuyển khoản cho đơn hàng thành công!",
        ordersService.confirmOrderPayment(id, note));
  }

  /** API 8.9: — Thủ kho nhận hàng hoàn kho & Hoàn tất hủy đơn (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping("/{id:[0-9]+}/receive-return")
  public ApiResponse<
          com.wedbackendservice.nextgen.module.order.dto.client.response.OrderDetailResponse>
      receiveReturnOrder(
          @PathVariable Long id,
          @RequestParam(required = false) String note,
          @RequestParam(required = false, defaultValue = "INTACT") String condition) {
    boolean isDamaged = "DAMAGED".equalsIgnoreCase(condition);
    String message =
        isDamaged
            ? "Đã ghi nhận biên bản MB04 chuyển kho kiểm định/thanh lý và cập nhật Đã hủy đơn!"
            : "Đã tiếp nhận kiện hàng nguyên vẹn (MB04), cộng lại tồn khả dụng và cập nhật Đã hủy đơn!";
    return ApiResponse.ok(message, ordersService.receiveReturnOrder(id, note, condition));
  }
}
