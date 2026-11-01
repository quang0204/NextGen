package com.wedbackendservice.nextgen.dto.response.shipment;

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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ShipmentTrackItem {
        String productName;
        String color;
        Integer quantity;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ShipmentTimelineItem {
        LocalDateTime time;
        String status;
        String description;
    }
}
