package com.wedbackendservice.nextgen.module.shipment.dto.client.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipmentTrackItem {
  String productName;
  String color;
  Integer quantity;
}
