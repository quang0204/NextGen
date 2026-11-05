package com.wedbackendservice.nextgen.dto.portal.response.inventory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryItemResponse {
    Long inventoryId;
    Long variantId;
    Long productId;
    String productName;
    String sku;
    String color;
    Integer romGb;
    Integer onHandQty;
    Integer reservedQty;
    Integer availableQty;
    Integer minThreshold;
    String status;
    String updatedAt;
}
