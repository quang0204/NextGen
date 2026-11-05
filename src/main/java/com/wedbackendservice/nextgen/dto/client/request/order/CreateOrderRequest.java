package com.wedbackendservice.nextgen.dto.client.request.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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

    @Builder.Default
    String paymentMethod = "COD";

    String voucherCode;

    String note;

    @NotEmpty(message = "Danh sách sản phẩm (items) không được để trống")
    @Valid
    List<OrderItemRequest> items;
}
