package com.wedbackendservice.nextgen.module.cart.dto.client.response;

import com.wedbackendservice.nextgen.core.dto.PageResponse;
import com.wedbackendservice.nextgen.module.product.dto.client.response.WishlistProductResponse;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WishlistListResponse {
  List<WishlistProductResponse> items;
  long total;
  PageResponse<?> pageInfo;
}
