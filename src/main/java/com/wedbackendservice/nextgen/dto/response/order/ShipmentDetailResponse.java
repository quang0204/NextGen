package com.wedbackendservice.nextgen.dto.response.order;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipmentDetailResponse {
    Long shipmentId;
    String trackingCode;
    String carrier;
    String status;
    String recipientName;
    String recipientPhone;
    BigDecimal codAmount;
}
