package com.wedbackendservice.nextgen.core.listener.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.core.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.core.event.saga.PaymentResultEvent;
import com.wedbackendservice.nextgen.core.event.saga.ReleaseStockEvent;
import com.wedbackendservice.nextgen.core.event.saga.StockReleasedEvent;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.module.notification.service.InboxService;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaListener {

  private final OrderRepository orderRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final OutboxService outboxService;
  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  /**
   * Bước 10-14: Nhận PaymentSucceeded / PaymentFailed - Nếu thành công: Order chuyển sang
   * WAITING_CONFIRMATION (chờ portal duyệt) - Nếu thất bại: Order bắn lệnh ReleaseStock sang Kafka
   * (bước 14)
   */
  @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "nextgen-order-saga")
  @Transactional
  public void handlePaymentResult(String payload) {
    try {
      PaymentResultEvent event = objectMapper.readValue(payload, PaymentResultEvent.class);
      String consumerGroup = "nextgen-order-saga";

      if (inboxService.isAlreadyProcessed(event.getEventId(), consumerGroup)) {
        log.info("[OrderSaga] Event {} already processed, skipping", event.getEventId());
        return;
      }

      log.info(
          "[OrderSaga] Received PaymentResult for order #{}, success: {}",
          event.getOrderCode(),
          event.isSuccess());

      Order order = orderRepository.findById(event.getOrderId()).orElse(null);
      if (order == null) {
        log.error("[OrderSaga] Order ID {} not found", event.getOrderId());
        return;
      }

      LocalDateTime now = LocalDateTime.now();

      if (event.isSuccess()) {
        String prevStatus = order.getStatus();
        boolean isTransfer =
            "BANK_TRANSFER".equalsIgnoreCase(event.getPaymentMethod())
                || "SIMULATED_TRANSFER".equalsIgnoreCase(event.getPaymentMethod());

        String nextStatus =
            isTransfer && !"PAID".equalsIgnoreCase(order.getPaymentStatus())
                ? "PENDING_PAYMENT"
                : "WAITING_CONFIRMATION";

        if (!"PENDING".equalsIgnoreCase(prevStatus) || nextStatus.equalsIgnoreCase(prevStatus)) {
          log.info(
              "[OrderSaga] Order #{} is already in state {} (target {}), skipping transition",
              order.getOrderCode(),
              prevStatus,
              nextStatus);
          inboxService.markAsProcessed(
              event.getEventId(), consumerGroup, "PaymentSucceeded", event.getOrderCode());
          return;
        }

        order.setStatus(nextStatus);
        order.setPaymentStatus(
            "COD".equalsIgnoreCase(event.getPaymentMethod())
                ? "UNPAID"
                : (order.getPaymentStatus() != null ? order.getPaymentStatus() : "UNPAID"));
        order.setUpdatedAt(now);
        orderRepository.save(order);

        orderStatusHistoryRepository.save(
            OrderStatusHistory.builder()
                .order(order)
                .previousStatus(prevStatus)
                .newStatus(nextStatus)
                .note(
                    "Saga Kafka: Giữ tồn kho thành công. Đơn hàng chuyển sang "
                        + nextStatus
                        + " (Chờ nhân viên Portal duyệt xác nhận).")
                .build());

        outboxService.saveProcessedEvent(
            "ORDER",
            order.getOrderCode(),
            "OrderProcessingSucceeded",
            "order-events-audit",
            java.util.Map.of(
                "orderId",
                order.getOrderId(),
                "orderCode",
                order.getOrderCode(),
                "status",
                nextStatus,
                "paymentMethod",
                event.getPaymentMethod() != null ? event.getPaymentMethod() : "COD",
                "message",
                "Đơn hàng đã điều phối tồn kho và thanh toán thành công qua Saga"));

        log.info(
            "[OrderSaga] Order #{} transitioned to {} via Saga (waiting for portal confirmation)",
            order.getOrderCode(),
            nextStatus);
      } else {
        log.warn(
            "[OrderSaga] Payment failed for order #{}: emitting ReleaseStock event",
            order.getOrderCode());
        ReleaseStockEvent releaseStockEvent =
            ReleaseStockEvent.builder()
                .correlationId(event.getCorrelationId())
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .reason(event.getMessage())
                .items(event.getItems())
                .build();

        outboxService.saveEvent(
            "ORDER",
            order.getOrderCode(),
            "ReleaseStock",
            KafkaTopics.RELEASE_STOCK,
            releaseStockEvent);
      }

      inboxService.markAsProcessed(
          event.getEventId(), consumerGroup, "PaymentResult", event.getOrderCode());

    } catch (Exception e) {
      log.error("[OrderSaga] Error processing PaymentResult: {}", e.getMessage(), e);
    }
  }

  /** Bước 18-19: Nhận StockReleased -> Cập nhật Order sang CANCELLED */
  @KafkaListener(topics = KafkaTopics.STOCK_RELEASED, groupId = "nextgen-order-cancel")
  @Transactional
  public void handleStockReleased(String payload) {
    try {
      StockReleasedEvent event = objectMapper.readValue(payload, StockReleasedEvent.class);
      String consumerGroup = "nextgen-order-cancel";

      if (inboxService.isAlreadyProcessed(event.getEventId(), consumerGroup)) {
        log.info(
            "[OrderSaga] Event {} already processed, skipping cancellation", event.getEventId());
        return;
      }

      log.info(
          "[OrderSaga] Received StockReleased for order #{}, finalizing cancellation",
          event.getOrderCode());

      Order order = orderRepository.findById(event.getOrderId()).orElse(null);
      if (order == null) {
        log.error("[OrderSaga] Order ID {} not found", event.getOrderId());
        return;
      }

      String prevStatus = order.getStatus();
      if (!"CANCELLED".equalsIgnoreCase(prevStatus)) {
        order.setStatus("CANCELLED");
        order.setCancelReason(
            event.getMessage() != null
                ? event.getMessage()
                : "Thanh toán thất bại, đã hoàn lại tồn kho qua Saga");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        orderStatusHistoryRepository.save(
            OrderStatusHistory.builder()
                .order(order)
                .previousStatus(prevStatus)
                .newStatus("CANCELLED")
                .note(
                    "Saga Kafka: Tồn kho đã được hoàn lại đầy đủ. Đơn hàng chuyển sang CANCELLED.")
                .build());

        log.info(
            "[OrderSaga] Order #{} successfully CANCELLED via Saga compensation",
            order.getOrderCode());
      }

      inboxService.markAsProcessed(
          event.getEventId(), consumerGroup, "StockReleased", event.getOrderCode());

    } catch (Exception e) {
      log.error("[OrderSaga] Error processing StockReleased: {}", e.getMessage(), e);
    }
  }
}
