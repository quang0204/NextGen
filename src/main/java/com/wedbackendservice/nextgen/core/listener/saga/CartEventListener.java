package com.wedbackendservice.nextgen.core.listener.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.core.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.core.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.module.notification.service.InboxService;
import com.wedbackendservice.nextgen.module.order.port.out.cart.CartPort;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Event-driven consumer dọn giỏ hàng bất đồng bộ sau khi đơn hàng đã commit. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventListener {

  private static final String CONSUMER_GROUP = "nextgen-cart-cleanup";

  private final CartPort cartPort;
  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = CONSUMER_GROUP)
  @Transactional
  public void handleOrderCreated(String payload) {
    try {
      OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
      if (inboxService.isAlreadyProcessed(event.getEventId(), CONSUMER_GROUP)) {
        return;
      }

      if (event.getUserId() != null && event.getItems() != null && !event.getItems().isEmpty()) {
        Set<Long> variantIds =
            event.getItems().stream()
                .map(OrderCreatedEvent.OrderItemDto::getVariantId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        cartPort.clearCartItems(event.getUserId(), variantIds);
      }

      inboxService.markAsProcessed(
          event.getEventId(), CONSUMER_GROUP, "OrderCreatedCartCleanup", event.getOrderCode());
    } catch (Exception e) {
      log.error("[CartEvent] Lỗi dọn giỏ hàng từ OrderCreated: {}", e.getMessage(), e);
    }
  }
}
