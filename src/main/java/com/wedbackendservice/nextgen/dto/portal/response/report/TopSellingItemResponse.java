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
public class TopSellingItemResponse {
  int rank;
  Long productId;
  Long variantId;
  String sku;
  String productName;
  String color;
  String rom;
  String imageUrl;
  BigDecimal unitPrice;
  long unitsSold;
  BigDecimal totalRevenue;
  int revenueShare;
  int onHandQty;
  int availableQty;
  int orderCount;
}
