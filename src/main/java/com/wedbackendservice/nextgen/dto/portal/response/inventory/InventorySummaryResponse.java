package com.wedbackendservice.nextgen.dto.portal.response.inventory;

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
