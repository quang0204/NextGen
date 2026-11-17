package com.wedbackendservice.nextgen.port.out.payment;

import com.wedbackendservice.nextgen.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentPort {
  void createInitialPayment(
      Order order, BigDecimal amount, String paymentMethod, String paymentProvider);

  Optional<PaymentInfoDto> getLatestPayment(Long orderId);

  void updatePaymentStatus(Long orderId, String newStatus);

  PaymentInfoDto confirmPaymentSuccess(
      Long orderId,
      BigDecimal amount,
      String transactionRef,
      LocalDateTime paidAt,
      String provider,
      String method);
}
