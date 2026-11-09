package com.wedbackendservice.nextgen.dto.client.response.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentIntentResponse {
    Long paymentId;
    Long orderId;
    String orderCode;
    String paymentMethod;
    BigDecimal amount;
    String status;
    String transactionRef;

    String bankName;
    String bankAccountNumber;
    String bankAccountName;
    String bankBranch;
    String transferContent;
    String qrContent;

    String message;
    LocalDateTime expiresAt;
    LocalDateTime createdAt;
}
