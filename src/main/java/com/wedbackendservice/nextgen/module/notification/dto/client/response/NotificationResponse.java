package com.wedbackendservice.nextgen.module.notification.dto.client.response;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
  Long notificationId;
  Long orderId;
  String orderCode;
  String title;
  String content;
  Boolean isRead;
  LocalDateTime createdAt;
}
