package com.wedbackendservice.nextgen.module.cart.dto.client.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartSyncRequest {
  @JsonProperty("guestItems")
  @JsonAlias({"guestItems", "items"})
  @Size(max = 50, message = "Tối đa đồng bộ 50 sản phẩm một lần")
  List<CartItemAddRequest> guestItems;
}
