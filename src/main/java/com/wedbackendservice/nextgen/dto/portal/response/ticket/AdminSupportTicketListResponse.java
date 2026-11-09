package com.wedbackendservice.nextgen.dto.portal.response.ticket;

import com.wedbackendservice.nextgen.dto.client.response.ticket.SupportTicketResponse;
import com.wedbackendservice.nextgen.dto.common.PageInfoResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminSupportTicketListResponse {
    List<SupportTicketResponse> items;
    PageInfoResponse pageInfo;
    Map<String, Long> statusCounts;
}
