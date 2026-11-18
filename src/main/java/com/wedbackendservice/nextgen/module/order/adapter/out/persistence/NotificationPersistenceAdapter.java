package com.wedbackendservice.nextgen.module.order.adapter.out.persistence;

import com.wedbackendservice.nextgen.module.notification.entity.Notification;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.port.out.notification.NotificationPort;
import com.wedbackendservice.nextgen.module.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationPersistenceAdapter implements NotificationPort {

  private final NotificationRepository notificationRepository;
  private final EntityManager entityManager;

  @Autowired
  public NotificationPersistenceAdapter(
      NotificationRepository notificationRepository, EntityManager entityManager) {
    this.notificationRepository = notificationRepository;
    this.entityManager = entityManager;
  }

  public NotificationPersistenceAdapter(NotificationRepository notificationRepository) {
    this(notificationRepository, null);
  }

  @Override
  public void sendNotification(Long userId, Long orderId, String title, String content) {
    if (userId == null) return;
    try {
      User userRef =
          entityManager != null
              ? entityManager.find(User.class, userId)
              : User.builder().userId(userId).build();
      Order orderRef = null;
      if (orderId != null) {
        orderRef =
            entityManager != null
                ? entityManager.find(Order.class, orderId)
                : Order.builder().orderId(orderId).version(0L).build();
      }
      Notification notif =
          Notification.builder()
              .user(userRef)
              .order(orderRef)
              .title(title)
              .content(content)
              .isRead(false)
              .build();
      notificationRepository.save(notif);
    } catch (Exception ex) {
      log.warn(
          "[NotificationAdapter] Gửi thông báo thất bại cho user {}: {}",
          userId,
          ex.getMessage(),
          ex);
    }
  }
}
