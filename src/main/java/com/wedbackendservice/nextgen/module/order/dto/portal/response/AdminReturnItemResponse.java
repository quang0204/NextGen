package com.wedbackendservice.nextgen.module.order.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wedbackendservice.nextgen.module.order.dto.client.request.BankInfoRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.response.OrderItemResponse;
import java.math.BigDecimal;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
