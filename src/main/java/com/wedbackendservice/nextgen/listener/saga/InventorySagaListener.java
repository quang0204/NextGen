package com.wedbackendservice.nextgen.listener.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.event.saga.*;
import com.wedbackendservice.nextgen.service.shared.InboxService;
import com.wedbackendservice.nextgen.service.shared.InventoryService;
import com.wedbackendservice.nextgen.service.shared.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventorySagaListener {

  private final InventoryService inventoryService;
  private final OutboxService outboxService;
  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  /**
   * Bước 4-6: Nhận ReserveStock (OrderCreated) -> Giữ hàng nguyên tử -> Bắn StockReserved /
   * StockReservationFailed
   */
  @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "nextgen-inventory-saga")
  @Transactional
  public void handleOrderCreated(String payload) {
    try {
      OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
      String consumerGroup = "nextgen-inventory-saga";

      if (inboxService.isAlreadyProcessed(event.getEventId(), consumerGroup)) {
        log.info("[InventorySaga] Event {} already processed, skipping", event.getEventId());
        return;
      }

      log.info(
          "[InventorySaga] Received OrderCreated for order #{}: starting stock reservation",
          event.getOrderCode());

      boolean reserved =
          inventoryService.reserveStockForOrder(event.getOrderId(), event.getItems(), 30);

      if (!reserved) {
        log.warn("[InventorySaga] Stock reservation failed for order #{}", event.getOrderCode());
        StockReservedEvent reservedEvent =
            StockReservedEvent.builder()
                .correlationId(event.getCorrelationId())
                .orderId(event.getOrderId())
                .orderCode(event.getOrderCode())
                .paymentMethod(event.getPaymentMethod())
                .totalAmount(event.getTotalAmount())
                .success(false)
                .message("Không đủ số lượng khả dụng trong kho hoặc đơn hàng không hợp lệ!")
                .items(event.getItems())
                .build();

        outboxService.saveEvent(
            "INVENTORY",
            event.getOrderCode(),
            "StockReservationFailed",
            KafkaTopics.STOCK_RESERVED,
            reservedEvent);

        inboxService.markAsProcessed(
            event.getEventId(), consumerGroup, "OrderCreated", event.getOrderCode());
        return;
      }

      log.info("[InventorySaga] Successfully reserved stock for order #{}", event.getOrderCode());
      StockReservedEvent reservedEvent =
          StockReservedEvent.builder()
              .correlationId(event.getCorrelationId())
              .orderId(event.getOrderId())
              .orderCode(event.getOrderCode())
              .paymentMethod(event.getPaymentMethod())
              .totalAmount(event.getTotalAmount())
              .success(true)
              .message("Giữ chỗ tồn kho thành công")
              .items(event.getItems())
              .build();

      outboxService.saveEvent(
          "INVENTORY",
          event.getOrderCode(),
          "StockReserved",
          KafkaTopics.STOCK_RESERVED,
          reservedEvent);

      inboxService.markAsProcessed(
          event.getEventId(), consumerGroup, "OrderCreated", event.getOrderCode());

    } catch (Exception e) {
      log.error("[InventorySaga] Error processing OrderCreated: {}", e.getMessage(), e);
    }
  }

  /** Bước 15-17: Nhận ReleaseStock -> Hoàn tồn kho -> Bắn StockReleased */
  @KafkaListener(topics = KafkaTopics.RELEASE_STOCK, groupId = "nextgen-inventory-compensate")
  @Transactional
  public void handleReleaseStock(String payload) {
    try {
      ReleaseStockEvent event = objectMapper.readValue(payload, ReleaseStockEvent.class);
      String consumerGroup = "nextgen-inventory-compensate";

      if (inboxService.isAlreadyProcessed(event.getEventId(), consumerGroup)) {
        log.info(
            "[InventorySaga] Event {} already processed, skipping release", event.getEventId());
        return;
      }

      log.info(
          "[InventorySaga] Received ReleaseStock for order #{}, reason: {}",
          event.getOrderCode(),
          event.getReason());

      inventoryService.releaseStockForOrder(event.getOrderId(), event.getReason());

      StockReleasedEvent releasedEvent =
          StockReleasedEvent.builder()
              .correlationId(event.getCorrelationId())
              .orderId(event.getOrderId())
              .orderCode(event.getOrderCode())
              .success(true)
              .message("Đã hoàn lại số lượng tồn kho giữ chỗ")
              .build();

      outboxService.saveEvent(
          "INVENTORY",
          event.getOrderCode(),
          "StockReleased",
          KafkaTopics.STOCK_RELEASED,
          releasedEvent);

      inboxService.markAsProcessed(
          event.getEventId(), consumerGroup, "ReleaseStock", event.getOrderCode());
      log.info("[InventorySaga] Stock successfully released for order #{}", event.getOrderCode());

    } catch (Exception e) {
      log.error("[InventorySaga] Error processing ReleaseStock: {}", e.getMessage(), e);
    }
  }
}
