package com.wedbackendservice.nextgen.dto.response.shipment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentFailDeliveryResponse {
    Long shipmentId;
    String trackingCode;
    String shipmentStatus;
    String failReason;
    String orderStatus;
    Boolean inventoryRestocked;
    Integer restockedItemsCount;
}
