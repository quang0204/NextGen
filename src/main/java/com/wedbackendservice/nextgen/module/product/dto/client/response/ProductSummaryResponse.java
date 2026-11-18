package com.wedbackendservice.nextgen.module.product.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSummaryResponse {
  Long productId;
  String name;
  BrandSummary brand;
  CategorySummary category;
  String primaryImage;
  BigDecimal minPrice;
  BigDecimal maxPrice;
  Integer totalVariants;
  Integer soldCount;
  List<String> availableColors;
  Boolean isAvailable;
  String status;
  List<ProductDetailResponse.VariantDetailResponse> variants;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class BrandSummary {
    Long brandId;
    String name;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class CategorySummary {
    Long categoryId;
    String name;
  }
}
