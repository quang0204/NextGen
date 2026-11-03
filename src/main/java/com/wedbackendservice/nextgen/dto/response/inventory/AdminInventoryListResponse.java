package com.wedbackendservice.nextgen.dto.response.inventory;

import com.wedbackendservice.nextgen.dto.response.admin.PaginationResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminInventoryListResponse {
    List<InventoryItemResponse> items;
    PaginationResponse pagination;
    InventorySummaryResponse summary;
}
