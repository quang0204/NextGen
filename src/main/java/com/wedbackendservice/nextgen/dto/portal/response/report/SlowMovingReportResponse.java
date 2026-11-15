package com.wedbackendservice.nextgen.dto.portal.response.report;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlowMovingReportResponse {
    List<SlowMovingItemResponse> items;
    int totalCount;
    BigDecimal totalSlowCapital;
    long totalSlowStock;
}
