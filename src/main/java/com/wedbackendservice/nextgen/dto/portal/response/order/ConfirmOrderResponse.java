package com.wedbackendservice.nextgen.dto.portal.response.order;

import com.wedbackendservice.nextgen.dto.client.response.order.ShipmentDetailResponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfirmOrderResponse {
    Long orderId;
    String orderCode;
    String orderStatus;
    String formCode;
    ShipmentDetailResponse shipment;
    String confirmedBy;
    String confirmedAt;
}
