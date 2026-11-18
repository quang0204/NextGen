package com.wedbackendservice.nextgen.module.notification.dto.portal.response;

import com.wedbackendservice.nextgen.core.dto.PageResponse;
import com.wedbackendservice.nextgen.module.notification.dto.client.response.SupportTicketResponse;
import java.util.List;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminSupportTicketListResponse {
  List<SupportTicketResponse> items;
  PageResponse<?> pageInfo;
  Map<String, Long> statusCounts;
}
