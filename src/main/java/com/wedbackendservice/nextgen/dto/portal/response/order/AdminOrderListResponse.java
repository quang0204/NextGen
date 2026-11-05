package com.wedbackendservice.nextgen.dto.portal.response.order;

import com.wedbackendservice.nextgen.dto.common.PageResponse;
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
