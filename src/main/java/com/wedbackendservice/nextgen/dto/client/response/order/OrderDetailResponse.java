package com.wedbackendservice.nextgen.dto.client.response.order;

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
public class OrderDetailResponse {
    Long orderId;
    String orderCode;
    String status;
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    String paymentMethod;
    String paymentStatus;
    BigDecimal subtotal;
    BigDecimal voucherDiscount;
    BigDecimal shippingFee;
    BigDecimal totalAmount;
    String carrier;
    String trackingCode;
    String cancelReason;
    Long returnId;
    String returnStatus;
    String returnReason;
    String returnRejectReason;
    String returnInspectionResult;
    BigDecimal returnRefundAmount;
    String createdAt;
    String updatedAt;
    String completedAt;
    List<OrderItemResponse> items;
    List<OrderStatusHistoryResponse> statusHistory;
}
