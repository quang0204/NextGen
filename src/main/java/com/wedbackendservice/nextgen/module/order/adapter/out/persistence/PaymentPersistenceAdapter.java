package com.wedbackendservice.nextgen.module.order.adapter.out.persistence;

import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.port.out.payment.PaymentInfoDto;
import com.wedbackendservice.nextgen.module.order.port.out.payment.PaymentPort;
import com.wedbackendservice.nextgen.module.payment.entity.Payment;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentPort {

  private final PaymentRepository paymentRepository;

  @Override
  public void createInitialPayment(
      Order order, BigDecimal amount, String paymentMethod, String paymentProvider) {
    Payment initialPayment =
        Payment.builder()
            .order(order)
            .paymentMethod(paymentMethod)
            .paymentProvider(paymentProvider)
            .amount(amount)
            .status(PaymentStatus.PENDING.name())
            .build();
    paymentRepository.save(initialPayment);
  }

  @Override
  public Optional<PaymentInfoDto> getLatestPayment(Long orderId) {
    return paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId).map(this::toDto);
  }

  @Override
  public void updatePaymentStatus(Long orderId, String newStatus) {
    paymentRepository
        .findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId)
        .ifPresent(
            payment -> {
              payment.setStatus(newStatus);
              paymentRepository.save(payment);
            });
  }

  @Override
  public PaymentInfoDto confirmPaymentSuccess(
      Long orderId,
      BigDecimal amount,
      String transactionRef,
      LocalDateTime paidAt,
      String provider,
      String method) {
    Optional<Payment> paymentOpt =
        paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId);
    Payment payment =
        paymentOpt.orElseGet(
            () ->
                Payment.builder()
                    .order(Order.builder().orderId(orderId).build())
                    .paymentMethod(method)
                    .paymentProvider(provider)
                    .amount(amount)
                    .build());
    payment.setAmount(amount);
    payment.setTransactionRef(transactionRef);
    payment.setStatus(PaymentStatus.SUCCESS.name());
    payment.setPaidAt(paidAt);
    Payment saved = paymentRepository.save(payment);
    return toDto(saved);
  }

  private PaymentInfoDto toDto(Payment payment) {
    return PaymentInfoDto.builder()
        .paymentId(payment.getPaymentId())
        .orderId(payment.getOrder() != null ? payment.getOrder().getOrderId() : null)
        .status(payment.getStatus())
        .amount(payment.getAmount())
        .paymentMethod(payment.getPaymentMethod())
        .paymentProvider(payment.getPaymentProvider())
        .transactionRef(payment.getTransactionRef())
        .paidAt(payment.getPaidAt())
        .build();
  }
}
