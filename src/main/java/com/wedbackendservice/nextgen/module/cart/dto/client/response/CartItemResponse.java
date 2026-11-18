package com.wedbackendservice.nextgen.module.cart.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {
  Long cartItemId;
  Long variantId;
  Long productId;
  String productName;
  String sku;
  String color;
  Integer ramGb;
  Integer romGb;
  String imageUrl;
  BigDecimal listPrice;
  BigDecimal currentPrice;
  Boolean isFlashSale;
  Integer quantity;
  BigDecimal lineTotal;
  Integer availableStock;
  Boolean inStock;
  LocalDateTime addedAt;
}
