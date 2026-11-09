package com.wedbackendservice.nextgen.dto.portal.response.admin;

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
public class AdminPaymentItemResponse {
    Long paymentId;
    Long orderId;
    String orderCode;
    String customerName;
    String customerPhone;
    String paymentMethod;
    BigDecimal amount;
    String status;
    String transactionRef;
    LocalDateTime paidAt;
    LocalDateTime createdAt;
}
