package com.wedbackendservice.nextgen.dto.portal.request.order;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PackExportOrderRequest {
    Integer packageWeightGrams;
    String exportNote;
    String assignedShipper;
}
