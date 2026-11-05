package com.wedbackendservice.nextgen.dto.client.response.shipment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipmentTrackResponse {
    Long shipmentId;
    String trackingCode;
    String orderCode;
    String carrier;
    String status;
    String statusLabel;
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    LocalDateTime estimatedDeliveryDate;
    List<ShipmentTrackItem> items;
    List<ShipmentTimelineItem> timeline;
}
