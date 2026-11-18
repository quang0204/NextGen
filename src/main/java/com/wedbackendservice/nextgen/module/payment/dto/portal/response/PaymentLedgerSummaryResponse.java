package com.wedbackendservice.nextgen.module.payment.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
