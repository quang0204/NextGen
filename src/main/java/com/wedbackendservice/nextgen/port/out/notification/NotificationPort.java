package com.wedbackendservice.nextgen.port.out.notification;

public interface NotificationPort {
  void sendNotification(Long userId, Long orderId, String title, String content);
}
