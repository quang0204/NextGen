package com.wedbackendservice.nextgen.dto.response.order;

import com.wedbackendservice.nextgen.dto.response.PageResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminOrderListResponse extends PageResponse<AdminOrderItemResponse> {
    PipelineSummaryResponse pipelineSummary;
}
