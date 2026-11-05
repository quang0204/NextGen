package com.wedbackendservice.nextgen.dto.portal.response.inventory;

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
public class AdminInventoryListResponse extends PageResponse<InventoryItemResponse> {
    InventorySummaryResponse summary;
}
