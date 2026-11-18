package com.wedbackendservice.nextgen.module.shipment.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
