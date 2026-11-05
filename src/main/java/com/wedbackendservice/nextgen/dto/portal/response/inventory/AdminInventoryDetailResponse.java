package com.wedbackendservice.nextgen.dto.portal.response.inventory;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminInventoryDetailResponse {
    Long inventoryId;
    Long variantId;
    Long productId;
    String productName;
    String sku;
    String color;
    Integer ramGb;
    Integer romGb;
    String connectionType;
    BigDecimal listPrice;
    Integer onHandQty;
    Integer reservedQty;
    Integer availableQty;
    Integer minThreshold;
    String status;
    List<InventoryTransactionItemResponse> recentTransactions;
    String updatedAt;
}
