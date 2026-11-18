package com.wedbackendservice.nextgen.module.product.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateVariantResponse {
  Long variantId;
  String sku;
  String variantName;
  String color;
  Integer ramGb;
  Integer romGb;
  String connectionType;
  Map<String, Object> attributes;
  Integer minThreshold;
  BigDecimal listPrice;
  BigDecimal salePrice;
  String imageUrl;
  Boolean isActive;
  String updatedAt;
}
