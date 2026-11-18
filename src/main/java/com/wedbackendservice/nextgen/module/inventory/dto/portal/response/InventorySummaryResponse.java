package com.wedbackendservice.nextgen.module.inventory.dto.portal.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventorySummaryResponse {
  Integer totalOnHand;
  Integer totalReserved;
  Integer totalAvailable;
  Integer lowStockCount;
}
