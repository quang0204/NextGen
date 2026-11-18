package com.wedbackendservice.nextgen.module.order.dto.client.response;

import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
