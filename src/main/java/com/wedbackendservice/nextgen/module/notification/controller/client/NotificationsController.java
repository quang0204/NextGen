package com.wedbackendservice.nextgen.module.notification.controller.client;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.notification.dto.client.response.NotificationListResponse;
import com.wedbackendservice.nextgen.module.notification.dto.client.response.NotificationReadResponse;
import com.wedbackendservice.nextgen.module.notification.service.NotificationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationsController {

  private final NotificationsService notificationsService;

  /** API 13.3: Danh sách thông báo. */
  @GetMapping("/unread-count")
  public ApiResponse<Long> getUnreadCount() {
    return ApiResponse.ok(notificationsService.getUnreadCount());
  }

  @GetMapping
  public ApiResponse<NotificationListResponse> getNotifications(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.ok(notificationsService.getMyNotifications(page, size));
  }

  /** API 13.4: Đánh dấu một thông báo đã đọc. */
  @PatchMapping("/{id}/read")
  public ApiResponse<NotificationReadResponse> markRead(@PathVariable Long id) {
    return ApiResponse.ok("Đã đánh dấu thông báo là đã đọc", notificationsService.markRead(id));
  }

  /** API 13.4 phụ: Đánh dấu tất cả đã đọc. */
  @PatchMapping("/read-all")
  public ApiResponse<NotificationReadResponse> markAllRead() {
    return ApiResponse.ok(
        "Đã đánh dấu tất cả thông báo là đã đọc", notificationsService.markAllRead());
  }

  /** Xóa thông báo. */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    notificationsService.delete(id);
    return ApiResponse.<Void>ok("Xóa thông báo thành công", null);
  }
}
