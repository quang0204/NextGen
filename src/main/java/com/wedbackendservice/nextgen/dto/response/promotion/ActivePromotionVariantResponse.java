package com.wedbackendservice.nextgen.dto.response.promotion;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivePromotionVariantResponse {
    Long promoVariantId;
    Long variantId;
    Long productId;
    String productName;
    String sku;
    String color;
    Integer romGb;
    BigDecimal listPrice;
    BigDecimal promoPrice;
    Integer discountPercent;
    Integer availableQty;
}
