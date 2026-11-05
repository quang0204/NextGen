package com.wedbackendservice.nextgen.dto.response.payment;

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
    String paymentProvider;
    BigDecimal amount;
    String status;
    String transactionRef;

    /** URL thanh toán cho VNPay / MoMo. */
    String paymentUrl;

    /** Thông tin chuyển khoản cho BANK_TRANSFER. */
    String bankName;
    String bankAccountNumber;
    String bankAccountName;
    String bankBranch;
    String transferContent;

    String message;
    LocalDateTime expiresAt;
    LocalDateTime createdAt;
}
