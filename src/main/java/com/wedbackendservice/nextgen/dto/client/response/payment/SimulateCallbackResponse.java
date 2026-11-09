package com.wedbackendservice.nextgen.dto.client.response.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
