package com.wedbackendservice.nextgen.dto.portal.response.returnrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wedbackendservice.nextgen.dto.client.request.returnrequest.BankInfoRequest;
import com.wedbackendservice.nextgen.dto.client.response.order.OrderItemResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminReturnItemResponse {
    Long returnId;
    Long orderId;
    String orderCode;
    BigDecimal orderTotalAmount;
    CustomerInfoResponse customer;
    String reason;
    List<String> evidenceImages;
    String status;
    String inspectionResult;
    String inspectedBy;
    String inspectedAt;
    String rejectReason;
    BankInfoRequest bankInfo;
    List<OrderItemResponse> items;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    BigDecimal refundAmount;

    String createdAt;
}
