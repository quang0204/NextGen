package com.wedbackendservice.nextgen.module.order.dto.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemResponse {
  Long orderItemId;
  Long variantId;
  Long productId;
  String productNameSnapshot;
  String skuSnapshot;
  String colorSnapshot;
  String romSnapshot;
  BigDecimal unitPrice;
  Integer quantity;
  BigDecimal lineTotal;
  String imageUrl;

  @JsonProperty("productName")
  public String getProductName() {
    return productNameSnapshot;
  }

  @JsonProperty("sku")
  public String getSku() {
    return skuSnapshot;
  }
}
