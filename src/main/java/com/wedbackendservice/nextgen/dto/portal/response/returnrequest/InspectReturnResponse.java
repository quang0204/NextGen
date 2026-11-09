package com.wedbackendservice.nextgen.dto.portal.response.returnrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

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
