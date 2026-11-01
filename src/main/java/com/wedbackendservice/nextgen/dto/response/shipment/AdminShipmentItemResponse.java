package com.wedbackendservice.nextgen.dto.response.shipment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminShipmentItemResponse {
    Long shipmentId;
    Long orderId;
    String orderCode;
    String carrier;
    String trackingCode;
    String status;
    String failReason;
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    BigDecimal orderTotalAmount;
    String paymentMethod;
    String updatedBy;
    LocalDateTime updatedAt;
}
