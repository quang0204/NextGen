package com.wedbackendservice.nextgen.dto.response.notification;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationReadResponse {
    Long notificationId;
    boolean isRead;
    long unreadCount;
}
