package com.wedbackendservice.nextgen.dto.response.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartSyncResponse {
    Long cartId;
    Integer totalItems;
    Integer syncedCount;
    BigDecimal subtotal;
    List<SkippedItem> skippedItems;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SkippedItem {
        Long variantId;
        Integer requestedQuantity;
        String reason;
    }
}
