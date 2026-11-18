package com.wedbackendservice.nextgen.module.payment.dto.client.request;

import com.wedbackendservice.nextgen.module.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePaymentIntentRequest {

  @NotNull(message = "orderId không được để trống")
  Long orderId;

  @NotNull(message = "Phương thức thanh toán không được để trống")
  PaymentMethod paymentMethod;

  String note;
}
