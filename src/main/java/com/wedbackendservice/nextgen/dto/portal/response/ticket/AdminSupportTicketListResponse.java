package com.wedbackendservice.nextgen.dto.portal.response.ticket;

import com.wedbackendservice.nextgen.dto.client.response.ticket.SupportTicketResponse;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
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
