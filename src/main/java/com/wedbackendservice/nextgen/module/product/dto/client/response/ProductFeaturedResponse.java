package com.wedbackendservice.nextgen.module.product.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductFeaturedResponse {
  Long productId;
  String name;
  String primaryImage;
  BigDecimal originalPrice;
  BigDecimal salePrice;
  Integer discountPercent;
  Double rating;
  Integer reviewCount;
  PromotionSummary promotion;
  Boolean inStock;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class PromotionSummary {
    Long promotionId;
    String name;
    String endAt;
  }
}
