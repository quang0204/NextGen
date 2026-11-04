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
public class MyOrderItemResponse {
    Long orderId;
    String orderCode;
    String status;
    BigDecimal totalAmount;
    String paymentMethod;
    String paymentStatus;
    String createdAt;
    List<OrderItemResponse> items;
}
