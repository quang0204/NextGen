package com.wedbackendservice.nextgen.dto.response.shipment;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public  class ShipmentTrackItem {
    String productName;
    String color;
    Integer quantity;
}