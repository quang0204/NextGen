package com.wedbackendservice.nextgen.dto.response.order;

import com.wedbackendservice.nextgen.dto.response.admin.PaginationResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminOrderListResponse {
    List<AdminOrderItemResponse> items;
    PipelineSummaryResponse pipelineSummary;
    PaginationResponse pagination;
}
