package com.wedbackendservice.nextgen.dto.response.inventory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LowStockItemResponse {
    Long variantId;
    Long productId;
    String productName;
    String sku;
    String color;
    Integer onHandQty;
    Integer reservedQty;
    Integer availableQty;
    Integer minThreshold;
    Integer deficit;
    String status;
    Integer recommendImportQty;
}
