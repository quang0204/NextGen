package com.wedbackendservice.nextgen.dto.response.order;

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
    String createdAt;
    String updatedAt;
    String completedAt;
    List<OrderItemResponse> items;
    List<OrderStatusHistoryResponse> statusHistory;
}
