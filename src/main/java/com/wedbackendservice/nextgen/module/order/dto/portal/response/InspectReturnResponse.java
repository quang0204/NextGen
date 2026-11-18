package com.wedbackendservice.nextgen.module.order.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InspectReturnResponse {
  Long returnId;
  Long orderId;
  String orderCode;
  String status;
  String inspectionResult;
  BigDecimal refundAmount;
  String rejectReason;
  String inspectedBy;
  String inspectedAt;
  String approvedAt;
  InventoryRestockResponse inventoryRestock;
  Boolean inventoryRestocked;
}
