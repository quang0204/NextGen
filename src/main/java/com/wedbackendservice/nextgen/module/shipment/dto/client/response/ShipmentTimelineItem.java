package com.wedbackendservice.nextgen.module.shipment.dto.client.response;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipmentTimelineItem {
  LocalDateTime time;
  String status;
  String description;
}
