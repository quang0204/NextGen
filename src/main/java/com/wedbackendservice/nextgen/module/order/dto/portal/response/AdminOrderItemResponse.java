package com.wedbackendservice.nextgen.module.order.dto.portal.response;

import com.wedbackendservice.nextgen.module.order.dto.client.response.OrderItemResponse;
import com.wedbackendservice.nextgen.module.order.dto.client.response.OrderStatusHistoryResponse;
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
public class AdminOrderItemResponse {
  Long orderId;
  String orderCode;
  String recipientName;
  String recipientPhone;
  String shippingAddress;
  String paymentMethod;
  String paymentStatus;
  BigDecimal subtotal;
  BigDecimal voucherDiscount;
  BigDecimal shippingFee;
  BigDecimal totalAmount;
  String status;
  String carrier;
  String trackingCode;
  String cancelReason;
  Long returnId;
  String returnStatus;
  String returnReason;
  String returnRejectReason;
  String returnInspectionResult;
  BigDecimal returnRefundAmount;
  String itemsSummary;
  String createdAt;
  String updatedAt;
  String completedAt;
  List<OrderItemResponse> items;
  List<OrderStatusHistoryResponse> statusHistory;
}
