package com.wedbackendservice.nextgen.module.notification.dto.client.response;

import com.wedbackendservice.nextgen.core.dto.PageResponse;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationListResponse {
  List<NotificationResponse> items;
  long unreadCount;
  PageResponse<?> pageInfo;
}
