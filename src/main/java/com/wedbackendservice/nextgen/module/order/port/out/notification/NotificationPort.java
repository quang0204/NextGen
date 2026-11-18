package com.wedbackendservice.nextgen.module.order.port.out.notification;

public interface NotificationPort {
  void sendNotification(Long userId, Long orderId, String title, String content);
}
