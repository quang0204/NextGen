package com.wedbackendservice.nextgen.module.order.dto.client.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class CreateOrderRequest {
  @NotBlank(message = "Tên người nhận (recipientName) không được để trống")
  String recipientName;

  @NotBlank(message = "Số điện thoại người nhận (recipientPhone) không được để trống")
  String recipientPhone;

  @NotBlank(message = "Địa chỉ nhận hàng (shippingAddress) không được để trống")
  String shippingAddress;

  @Builder.Default String paymentMethod = "COD";

  @Builder.Default String shippingMethod = "standard";

  BigDecimal shippingFee;

  BigDecimal expectedTotalAmount;

  String voucherCode;

  String note;

  @NotEmpty(message = "Danh sách sản phẩm (items) không được để trống")
  @Valid
  List<OrderItemRequest> items;
}
