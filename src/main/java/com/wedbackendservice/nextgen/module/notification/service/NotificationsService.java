package com.wedbackendservice.nextgen.module.notification.service;

import com.wedbackendservice.nextgen.core.dto.PageResponse;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.module.notification.dto.client.response.NotificationListResponse;
import com.wedbackendservice.nextgen.module.notification.dto.client.response.NotificationReadResponse;
import com.wedbackendservice.nextgen.module.notification.entity.Notification;
import com.wedbackendservice.nextgen.module.notification.mapper.NotificationMapper;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationsService {

  private final NotificationRepository notificationRepository;
  private final SecurityHelper securityHelper;
  private final NotificationMapper notificationMapper;

  /** API 13.3: danh sách thông báo của tôi. */
  @Transactional(readOnly = true)
  public long getUnreadCount() {
    User user = securityHelper.getCurrentUser();
    return notificationRepository.countByUser_UserIdAndIsReadFalse(user.getUserId());
  }

  @Transactional(readOnly = true)
  public NotificationListResponse getMyNotifications(int page, int size) {
    User user = securityHelper.getCurrentUser();
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(size, 1), 100);
    Pageable pageable =
        PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<Notification> result =
        notificationRepository.findByUser_UserId(user.getUserId(), pageable);
    long unread = notificationRepository.countByUser_UserIdAndIsReadFalse(user.getUserId());

    return NotificationListResponse.builder()
        .items(result.getContent().stream().map(notificationMapper::toResponse).toList())
        .unreadCount(unread)
        .pageInfo(PageResponse.of(safePage, safeSize, result))
        .build();
  }

  /** API 13.4: đánh dấu một thông báo đã đọc. */
  @Transactional
  public NotificationReadResponse markRead(Long notificationId) {
    User user = securityHelper.getCurrentUser();
    Notification notification = requireOwnNotification(notificationId, user);
    notification.setIsRead(true);
    notificationRepository.save(notification);

    return NotificationReadResponse.builder()
        .notificationId(notificationId)
        .isRead(true)
        .unreadCount(notificationRepository.countByUser_UserIdAndIsReadFalse(user.getUserId()))
        .build();
  }

  /** API 13.4 phụ: đánh dấu tất cả đã đọc. */
  @Transactional
  public NotificationReadResponse markAllRead() {
    User user = securityHelper.getCurrentUser();
    notificationRepository.markAllAsRead(user.getUserId());

    return NotificationReadResponse.builder().isRead(true).unreadCount(0).build();
  }

  /** Xóa thông báo. */
  @Transactional
  public void delete(Long notificationId) {
    User user = securityHelper.getCurrentUser();
    notificationRepository.delete(requireOwnNotification(notificationId, user));
  }

  private Notification requireOwnNotification(Long notificationId, User user) {
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(
                () -> new AppException(ErrorCode.INVALID_REQUEST, "Thông báo không tồn tại."));
    if (notification.getUser() == null
        || !user.getUserId().equals(notification.getUser().getUserId())) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    return notification;
  }
}
