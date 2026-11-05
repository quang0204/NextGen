package com.wedbackendservice.nextgen.dto.portal.response.inventory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryTransactionItemResponse {
    Long txnId;
    String formCode;
    String sku;
    String productName;
    String txnType;
    Integer quantityChange;
    Integer qtyBefore;
    Integer qtyAfter;
    Long orderId;
    String reason;
    String performedBy;
    String performedAt;
}
