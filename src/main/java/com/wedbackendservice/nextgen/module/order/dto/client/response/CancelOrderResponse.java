package com.wedbackendservice.nextgen.module.order.dto.client.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelOrderResponse {
  String orderCode;
  String status;
  String cancelReason;
  Boolean releasedHoldStock;
  String cancelledAt;
}
