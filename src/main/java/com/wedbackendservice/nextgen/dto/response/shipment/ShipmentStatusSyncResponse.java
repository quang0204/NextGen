package com.wedbackendservice.nextgen.dto.response.shipment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentStatusSyncResponse {
    Long shipmentId;
    String trackingCode;
    String shipmentStatus;
    Long synchronizedOrderId;
    String synchronizedOrderStatus;
    Boolean isPaymentCompleted;
    LocalDateTime updatedAt;
}
