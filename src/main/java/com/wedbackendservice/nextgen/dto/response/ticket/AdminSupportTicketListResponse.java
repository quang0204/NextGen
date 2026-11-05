package com.wedbackendservice.nextgen.dto.response.ticket;

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
    int page;
    int size;
    int totalPages;
    long totalElements;
    boolean hasNext;
    Map<String, Long> statusCounts;
}
