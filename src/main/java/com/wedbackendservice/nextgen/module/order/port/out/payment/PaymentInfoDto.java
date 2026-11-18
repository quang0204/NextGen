package com.wedbackendservice.nextgen.module.order.port.out.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentInfoDto {
  private final Long paymentId;
  private final Long orderId;
  private final String status;
  private final BigDecimal amount;
  private final String paymentMethod;
  private final String paymentProvider;
  private final String transactionRef;
  private final LocalDateTime paidAt;
}
