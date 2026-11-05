package com.wedbackendservice.nextgen.dto.portal.response.inventory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryAdjustmentResponse {
    Long adjustmentId;
    String formCode;
    Long variantId;
    String sku;
    String productName;
    Integer qtyBefore;
    Integer qtyActual;
    Integer quantityChange;
    Integer reservedQty;
    Integer availableQty;
    String reason;
    String performedBy;
    String performedAt;
}
