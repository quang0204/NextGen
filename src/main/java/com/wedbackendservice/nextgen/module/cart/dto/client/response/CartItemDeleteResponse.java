package com.wedbackendservice.nextgen.module.cart.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemDeleteResponse {
  Long deletedItemId;
  Integer remainingItems;
  BigDecimal newSubtotal;
}
