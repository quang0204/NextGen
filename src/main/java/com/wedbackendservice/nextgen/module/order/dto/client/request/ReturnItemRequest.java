package com.wedbackendservice.nextgen.module.order.dto.client.request;

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
public class ReturnItemRequest {
  Long variantId;
  String productName;
  String sku;
  Integer quantity;
  BigDecimal unitPrice;

  @JsonProperty("productNameSnapshot")
  public void setProductNameSnapshot(String productNameSnapshot) {
    if (this.productName == null || this.productName.isBlank()) {
      this.productName = productNameSnapshot;
    }
  }

  @JsonProperty("skuSnapshot")
  public void setSkuSnapshot(String skuSnapshot) {
    if (this.sku == null || this.sku.isBlank()) {
      this.sku = skuSnapshot;
    }
  }
}
