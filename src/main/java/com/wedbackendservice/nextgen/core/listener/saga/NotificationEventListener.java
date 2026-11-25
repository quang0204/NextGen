package com.wedbackendservice.nextgen.core.listener.saga;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.core.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.core.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.core.event.saga.PaymentResultEvent;
import com.wedbackendservice.nextgen.core.event.saga.StockReleasedEvent;
import com.wedbackendservice.nextgen.module.notification.service.InboxService;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.port.out.notification.NotificationPort;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event-driven consumer xử lý toàn bộ thông báo đơn hàng bất đồng bộ qua Kafka để giữ Critical Path
 * đặt/hủy/duyệt đơn thật ngắn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private static final String CONSUMER_GROUP = "nextgen-notification-events";

  private final NotificationPort notificationPort;
  private final OrderRepository orderRepository;
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

      if (event.getUserId() != null && event.getOrderId() != null) {
        boolean isTransfer =
            "BANK_TRANSFER".equalsIgnoreCase(event.getPaymentMethod())
                || "SIMULATED_TRANSFER".equalsIgnoreCase(event.getPaymentMethod());
        int expireMinutes = event.getHoldMinutes() != null ? event.getHoldMinutes() : 30;

        String title;
        String content;
        if (!event.isSyncCompleted()) {
          title = "Đơn hàng đang được xử lý";
          content =
              "Đơn hàng "
                  + event.getOrderCode()
                  + " đã được tiếp nhận và đang kiểm tra giữ chỗ tồn kho qua hệ thống xử lý bất"
                  + " đồng bộ.";
        } else if (isTransfer) {
          title = "Đơn hàng đang chờ thanh toán";
          content =
              "Đơn hàng "
                  + event.getOrderCode()
                  + " đã được tạo. Vui lòng chuyển khoản trong vòng "
                  + expireMinutes
                  + " phút.";
        } else {
          title = "Đặt hàng thành công";
          content =
              "Đơn hàng "
                  + event.getOrderCode()
                  + " đã được tạo thành công và đang chờ xác nhận.";
        }

        notificationPort.sendNotification(event.getUserId(), event.getOrderId(), title, content);
      }

      inboxService.markAsProcessed(
          event.getEventId(), CONSUMER_GROUP, "OrderCreatedNotification", event.getOrderCode());
    } catch (Exception e) {
      log.error("[NotificationEvent] Lỗi gửi thông báo OrderCreated: {}", e.getMessage(), e);
    }
  }

  @KafkaListener(topics = KafkaTopics.ORDER_CONFIRMED, groupId = CONSUMER_GROUP)
  @Transactional
  public void handleOrderConfirmed(String payload) {
    try {
      Map<String, Object> data =
          objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
      String eventId = str(data.get("eventId"));
      if (inboxService.isAlreadyProcessed(eventId, CONSUMER_GROUP)) {
        return;
      }

      Long orderId = toLong(data.get("orderId"));
      Long userId = toLong(data.get("userId"));
      if (userId == null && orderId != null) {
        userId = orderRepository.findById(orderId).map(o -> o.getUser().getUserId()).orElse(null);
      }
      String orderCode = str(data.get("orderCode"));
      if (orderCode == null && orderId != null) {
        orderCode = "#" + orderId;
      }
      String carrier = str(data.get("carrier"));

      if (userId != null && orderId != null) {
        notificationPort.sendNotification(
            userId,
            orderId,
            "Đơn hàng đã được xác nhận",
            "Đơn hàng "
                + orderCode
                + " đã được xác nhận và đang được chuẩn bị để giao"
                + (carrier != null ? " cho " + carrier : "")
                + ".");
      }

      inboxService.markAsProcessed(eventId, CONSUMER_GROUP, "OrderConfirmedNotification", orderCode);
    } catch (Exception e) {
      log.error("[NotificationEvent] Lỗi gửi thông báo OrderConfirmed: {}", e.getMessage(), e);
    }
  }

  @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = CONSUMER_GROUP)
  @Transactional
  public void handleOrderCancelled(String payload) {
    try {
      Map<String, Object> data =
          objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
      String eventId = str(data.get("eventId"));
      if (inboxService.isAlreadyProcessed(eventId, CONSUMER_GROUP)) {
        return;
      }

      Long orderId = toLong(data.get("orderId"));
      Long userId = toLong(data.get("userId"));
      Order order =
          (userId == null && orderId != null)
              ? orderRepository.findById(orderId).orElse(null)
              : null;
      if (userId == null && order != null && order.getUser() != null) {
        userId = order.getUser().getUserId();
      }
      String orderCode = str(data.get("orderCode"));
      if (orderCode == null && order != null) {
        orderCode = order.getOrderCode();
      }
      String reason = str(data.get("cancelReason"));
      if (reason == null) {
        reason = str(data.get("reason"));
      }

      if (userId != null && orderId != null) {
        notificationPort.sendNotification(
            userId,
            orderId,
            "Đơn hàng đã bị hủy",
            "Đơn hàng "
                + (orderCode != null ? orderCode : "#" + orderId)
                + " đã bị hủy"
                + (reason != null ? ". Lý do: " + reason : "."));
      }

      inboxService.markAsProcessed(eventId, CONSUMER_GROUP, "OrderCancelledNotification", orderCode);
    } catch (Exception e) {
      log.error("[NotificationEvent] Lỗi gửi thông báo OrderCancelled: {}", e.getMessage(), e);
    }
  }

  @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = CONSUMER_GROUP)
  @Transactional
  public void handlePaymentEvent(String payload) {
    try {
      PaymentResultEvent event = objectMapper.readValue(payload, PaymentResultEvent.class);
      if (inboxService.isAlreadyProcessed(event.getEventId(), CONSUMER_GROUP)) {
        return;
      }

      if (event.isSuccess() && event.getOrderId() != null) {
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order != null && order.getUser() != null) {
          Long userId = order.getUser().getUserId();
          String orderCode = event.getOrderCode() != null ? event.getOrderCode() : order.getOrderCode();
          if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            notificationPort.sendNotification(
                userId,
                order.getOrderId(),
                "Thanh toán thành công",
                "Đơn hàng "
                    + orderCode
                    + " đã được xác nhận thanh toán thành công"
                    + (event.getAmount() != null ? " số tiền " + event.getAmount() + "đ." : "."));
          } else if (event.getMessage() != null && !event.getMessage().isBlank()) {
            notificationPort.sendNotification(
                userId,
                order.getOrderId(),
                "Cập nhật đơn hàng",
                "Đơn hàng " + orderCode + ": " + event.getMessage());
          }
        }
      }

      inboxService.markAsProcessed(
          event.getEventId(), CONSUMER_GROUP, "PaymentEventNotification", event.getOrderCode());
    } catch (Exception e) {
      log.error("[NotificationEvent] Lỗi gửi thông báo PaymentEvent: {}", e.getMessage(), e);
    }
  }

  @KafkaListener(topics = KafkaTopics.STOCK_RELEASED, groupId = CONSUMER_GROUP)
  @Transactional
  public void handleStockReleased(String payload) {
    try {
      StockReleasedEvent event = objectMapper.readValue(payload, StockReleasedEvent.class);
      if (inboxService.isAlreadyProcessed(event.getEventId(), CONSUMER_GROUP)) {
        return;
      }

      if (event.getOrderId() != null) {
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order != null && order.getUser() != null) {
          notificationPort.sendNotification(
              order.getUser().getUserId(),
              order.getOrderId(),
              "Đơn hàng đã bị hủy",
              "Đơn hàng "
                  + (event.getOrderCode() != null ? event.getOrderCode() : order.getOrderCode())
                  + " đã bị hủy do kiểm tra tồn kho hoặc thanh toán không thành công.");
        }
      }

      inboxService.markAsProcessed(
          event.getEventId(), CONSUMER_GROUP, "StockReleasedNotification", event.getOrderCode());
    } catch (Exception e) {
      log.error("[NotificationEvent] Lỗi gửi thông báo StockReleased: {}", e.getMessage(), e);
    }
  }

  private Long toLong(Object val) {
    if (val instanceof Number num) {
      return num.longValue();
    }
    if (val instanceof String s && !s.isBlank()) {
      try {
        return Long.parseLong(s.trim());
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private String str(Object val) {
    return val != null ? String.valueOf(val) : null;
  }
}
