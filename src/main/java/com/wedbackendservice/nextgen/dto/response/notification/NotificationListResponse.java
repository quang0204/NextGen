package com.wedbackendservice.nextgen.dto.response.notification;

import com.wedbackendservice.nextgen.dto.response.PageInfoResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationListResponse {
    List<NotificationResponse> items;
    long unreadCount;
    PageInfoResponse pageInfo;
}
