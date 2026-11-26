package com.wedbackendservice.nextgen.module.product.dto.portal.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateVariantRequest {
  @JsonAlias({"list_price", "listPrice", "compare_at_price", "compareAtPrice"})
  @Positive(message = "listPrice phải lớn hơn 0")
  BigDecimal listPrice;

  @JsonAlias({"sale_price", "salePrice", "price"})
  @Positive(message = "salePrice phải lớn hơn 0")
  BigDecimal salePrice;

  @JsonAlias({"compare_at_price", "compareAtPrice"})
  @Positive(message = "compareAtPrice phải lớn hơn 0")
  BigDecimal compareAtPrice;

  @JsonAlias({"price", "sale_price", "salePrice"})
  @Positive(message = "price phải lớn hơn 0")
  BigDecimal price;

  Boolean isActive;

  String sku;
  String color;
  Integer ramGb;
  Integer romGb;
  String connectionType;
  String imageUrl;
  Map<String, Object> attributes;
  Integer minThreshold;
}
