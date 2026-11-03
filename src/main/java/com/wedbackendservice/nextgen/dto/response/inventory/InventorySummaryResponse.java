package com.wedbackendservice.nextgen.dto.response.inventory;

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
