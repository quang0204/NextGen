package com.wedbackendservice.nextgen.module.product.dto.portal.response;

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
public class CreateProductResponse {
  Long productId;
  String name;
  Long categoryId;
  Long brandId;
  String status;
  Integer totalImages;
  Integer totalSpecs;
  List<CreatedVariantSummary> createdVariants;
  String createdAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class CreatedVariantSummary {
    Long variantId;
    String sku;
    BigDecimal listPrice;
    InventorySummary inventory;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class InventorySummary {
    Long inventoryId;
    Integer onHandQty;
    Integer reservedQty;
    Integer availableQty;
    Integer minThreshold;
  }
}
