package com.wedbackendservice.nextgen.module.order.dto.portal.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExportedItemResponse {
  String sku;
  Integer quantity;
  Integer remainingOnHand;
}
