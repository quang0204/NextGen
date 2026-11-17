package com.wedbackendservice.nextgen.dto.client.response.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {
  Long cartId;
  Long userId;
  List<CartItemResponse> items;
  Integer totalQuantity;
  BigDecimal subtotal;
  LocalDateTime updatedAt;
}
