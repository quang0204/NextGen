package com.wedbackendservice.nextgen.module.order.adapter.out.persistence;

import com.wedbackendservice.nextgen.module.notification.entity.Notification;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.port.out.notification.NotificationPort;
import com.wedbackendservice.nextgen.module.user.entity.User;
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
