package com.wedbackendservice.nextgen.module.voucher.dto.client.response;

import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
