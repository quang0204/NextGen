package com.wedbackendservice.nextgen.dto.client.response.order;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderResponse {
    Long orderId;
    String orderCode;
    String status;
    BigDecimal subtotal;
    BigDecimal voucherDiscount;
    BigDecimal shippingFee;
    BigDecimal totalAmount;
    String paymentMethod;
    String paymentStatus;
    Integer itemsCount;
    String createdAt;
}
