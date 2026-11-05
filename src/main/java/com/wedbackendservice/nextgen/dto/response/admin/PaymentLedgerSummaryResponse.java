package com.wedbackendservice.nextgen.dto.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentLedgerSummaryResponse {
    LocalDate fromDate;
    LocalDate toDate;
    long totalTransactions;
    long successTransactions;
    long pendingTransactions;
    long failedTransactions;
    long refundedTransactions;
    BigDecimal totalCollected;
    BigDecimal totalPending;
    BigDecimal totalRefunded;
    Map<String, BigDecimal> collectedByMethod;
}
