package com.wedbackendservice.nextgen.module.payment.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulateCallbackResponse {
  Long paymentId;
  String transactionRef;
  String status;
  Long orderId;
  String orderCode;
  String orderPaymentStatus;
  LocalDateTime paidAt;
  String message;
}
