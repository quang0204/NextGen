package com.wedbackendservice.nextgen.dto.response.order;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PackExportOrderResponse {
    Long orderId;
    String orderCode;
    String orderStatus;
    String formCode;
    Boolean inventoryDeducted;
    List<ExportedItemResponse> exportedItems;
    ShipmentDetailResponse shipment;
    String exportedBy;
    String exportedAt;
}
