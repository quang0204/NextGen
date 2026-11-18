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
public class ShipmentDetailResponse {
  Long shipmentId;
  String trackingCode;
  String carrier;
  String status;
  String recipientName;
  String recipientPhone;
  BigDecimal codAmount;
}
