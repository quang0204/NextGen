package com.wedbackendservice.nextgen.dto.portal.response.report;

import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlowMovingItemResponse {
  Long productId;
  Long variantId;
  String sku;
  String productName;
  String color;
  String rom;
  String imageUrl;
  int onHandQty;
  int availableQty;
  BigDecimal unitPrice;
  long unitsSold;
  BigDecimal tiedCapital;
  int daysInStock;
  String severity; // CRITICAL, WARNING, ATTENTION
  String recommendedPolicy;
  int suggestedDiscountPercent;
}
