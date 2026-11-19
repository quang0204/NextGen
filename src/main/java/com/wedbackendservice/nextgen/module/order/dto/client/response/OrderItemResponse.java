package com.wedbackendservice.nextgen.module.order.dto.client.response;

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
}
