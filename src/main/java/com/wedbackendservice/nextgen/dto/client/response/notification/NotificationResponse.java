package com.wedbackendservice.nextgen.dto.client.response.notification;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    Long notificationId;
    Long orderId;
    String title;
    String content;
    Boolean isRead;
    LocalDateTime createdAt;
}
