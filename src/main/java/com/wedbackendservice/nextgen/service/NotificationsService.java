package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.response.PageInfoResponse;
import com.wedbackendservice.nextgen.dto.response.notification.NotificationListResponse;
import com.wedbackendservice.nextgen.dto.response.notification.NotificationReadResponse;
import com.wedbackendservice.nextgen.dto.response.notification.NotificationResponse;
import com.wedbackendservice.nextgen.entity.Notification;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.NotificationRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
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

    /**
     * API 13.3: danh sách thông báo của tôi.
     */
    @Transactional(readOnly = true)
    public NotificationListResponse getMyNotifications(int page, int size) {
        User user = securityHelper.getCurrentUser();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> result = notificationRepository.findByUser_UserId(user.getUserId(), pageable);
        long unread = notificationRepository.countByUser_UserIdAndIsReadFalse(user.getUserId());

        return NotificationListResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .unreadCount(unread)
                .pageInfo(PageInfoResponse.of(safePage, safeSize, result))
                .build();
    }

    /**
     * API 13.4: đánh dấu một thông báo đã đọc.
     */
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

    /**
     * API 13.4 phụ: đánh dấu tất cả đã đọc.
     */
    @Transactional
    public NotificationReadResponse markAllRead() {
        User user = securityHelper.getCurrentUser();
        notificationRepository.markAllAsRead(user.getUserId());

        return NotificationReadResponse.builder()
                .isRead(true)
                .unreadCount(0)
                .build();
    }

    /**
     * Xóa thông báo.
     */
    @Transactional
    public void delete(Long notificationId) {
        User user = securityHelper.getCurrentUser();
        notificationRepository.delete(requireOwnNotification(notificationId, user));
    }

    private Notification requireOwnNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Thông báo không tồn tại."));
        if (notification.getUser() == null
                || !user.getUserId().equals(notification.getUser().getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return notification;
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .orderId(notification.getOrder() != null ? notification.getOrder().getOrderId() : null)
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
