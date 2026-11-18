package com.wedbackendservice.nextgen.module.cart.dto.client.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WishlistToggleRequest {

  @NotNull(message = "productId không được để trống")
  Long productId;
}
