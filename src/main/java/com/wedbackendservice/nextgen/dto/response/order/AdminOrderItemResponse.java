package com.wedbackendservice.nextgen.dto.response.order;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

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
    BigDecimal totalAmount;
    String status;
    String carrier;
    String trackingCode;
    String itemsSummary;
    String createdAt;
}
