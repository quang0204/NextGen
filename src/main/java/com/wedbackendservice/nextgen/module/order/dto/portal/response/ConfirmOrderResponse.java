package com.wedbackendservice.nextgen.module.order.dto.portal.response;

import com.wedbackendservice.nextgen.module.order.dto.client.response.ShipmentDetailResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfirmOrderResponse {
  Long orderId;
  String orderCode;
  String orderStatus;
  String formCode;
  ShipmentDetailResponse shipment;
  String confirmedBy;
  String confirmedAt;
}
