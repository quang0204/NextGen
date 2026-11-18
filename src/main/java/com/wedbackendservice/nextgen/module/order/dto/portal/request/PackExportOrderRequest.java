package com.wedbackendservice.nextgen.module.order.dto.portal.request;

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
