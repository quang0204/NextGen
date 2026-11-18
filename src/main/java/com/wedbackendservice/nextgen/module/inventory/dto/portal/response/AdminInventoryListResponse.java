package com.wedbackendservice.nextgen.module.inventory.dto.portal.response;

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
public class AdminInventoryListResponse extends PageResponse<InventoryItemResponse> {
  InventorySummaryResponse summary;
}
