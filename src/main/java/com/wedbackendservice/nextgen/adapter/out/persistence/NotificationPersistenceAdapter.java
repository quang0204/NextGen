package com.wedbackendservice.nextgen.adapter.out.persistence;

import com.wedbackendservice.nextgen.entity.Notification;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.port.out.notification.NotificationPort;
import com.wedbackendservice.nextgen.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationPort {

  private final NotificationRepository notificationRepository;

  @Override
  public void sendNotification(Long userId, Long orderId, String title, String content) {
    if (userId == null) return;
    try {
      Notification notif =
          Notification.builder()
              .user(User.builder().userId(userId).build())
              .order(orderId != null ? Order.builder().orderId(orderId).build() : null)
              .title(title)
              .content(content)
              .isRead(false)
              .build();
      notificationRepository.save(notif);
    } catch (Exception ex) {
      log.warn(
          "[NotificationAdapter] Gửi thông báo thất bại cho user {}: {}", userId, ex.getMessage());
    }
  }
}
