package com.wedbackendservice.nextgen.dto.response.ticket;

import com.wedbackendservice.nextgen.dto.response.PageInfoResponse;
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
