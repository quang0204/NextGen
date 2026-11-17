package com.wedbackendservice.nextgen.dto.client.response.shipment;

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
