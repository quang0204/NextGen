package com.wedbackendservice.nextgen.module.order.dto.portal.response;

import com.wedbackendservice.nextgen.module.order.dto.client.response.ShipmentDetailResponse;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
