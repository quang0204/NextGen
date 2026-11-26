package com.wedbackendservice.nextgen.module.product.dto.portal.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateVariantRequest {
  @NotBlank(message = "SKU không được để trống")
  String sku;

  String color;
  Integer ramGb;
  Integer romGb;
  String connectionType;
  String imageUrl;
  Map<String, Object> attributes;

  @NotNull(message = "listPrice không được để trống")
  @Positive(message = "listPrice phải lớn hơn 0")
  BigDecimal listPrice;

  @NotNull(message = "salePrice không được để trống")
  @Positive(message = "salePrice phải lớn hơn 0")
  BigDecimal salePrice;

  @Positive(message = "compareAtPrice phải lớn hơn 0")
  BigDecimal compareAtPrice;

  @NotNull(message = "initialOnHandQty không được để trống")
  @Positive(message = "initialOnHandQty phải lớn hơn 0")
  Integer initialOnHandQty;

  @PositiveOrZero(message = "minThreshold phải >= 0")
  Integer minThreshold;
}
