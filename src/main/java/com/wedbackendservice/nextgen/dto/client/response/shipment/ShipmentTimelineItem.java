package com.wedbackendservice.nextgen.dto.client.response.shipment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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