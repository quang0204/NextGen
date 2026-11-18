package com.wedbackendservice.nextgen.module.order.dto.portal.response;

import com.wedbackendservice.nextgen.core.dto.PageResponse;
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
