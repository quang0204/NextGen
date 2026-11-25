package com.wedbackendservice.nextgen.module.order.service;

import com.wedbackendservice.nextgen.core.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.module.inventory.service.InventoryService;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import com.wedbackendservice.nextgen.module.voucher.repository.VoucherRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationService {

  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final VoucherRepository voucherRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final InventoryService inventoryService;
  private final OutboxService outboxService;
  private final Clock clock;

  /**
   * Hủy một đơn hàng hết hạn thanh toán trong một Database Transaction độc lập. Đảm bảo một đơn lỗi
   * không làm rollback cả lô.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean cancelSingleExpiredOrder(Long orderId, int expireMins) {
    Order order = orderRepository.findById(orderId).orElse(null);
    if (order == null) {
      return false;
    }

    // Re-check nghiêm ngặt dưới transaction mới
    String currentStatus = order.getStatus();
    if (!OrderStatus.PENDING_PAYMENT.name().equalsIgnoreCase(currentStatus)
        || "PAID".equalsIgnoreCase(order.getPaymentStatus())
        || PaymentStatus.SUCCESS.name().equalsIgnoreCase(order.getPaymentStatus())
        || "CUSTOMER_NOTIFIED".equalsIgnoreCase(order.getPaymentStatus())
        || PaymentStatus.CUSTOMER_NOTIFIED.name().equalsIgnoreCase(order.getPaymentStatus())) {
      log.info(
          "[OrderExpirationService] Đơn hàng #{} đã thay đổi trạng thái ({}, {}), bỏ qua hủy tự động",
          orderId,
          currentStatus,
          order.getPaymentStatus());
      return false;
    }

    LocalDateTime now = LocalDateTime.now(clock);

    // 1. Rollback giải phóng tồn giữ chỗ (idempotent)
    inventoryService.releaseStockForOrder(
        order.getOrderId(), "Mã QR hết hiệu lực thanh toán (" + expireMins + " phút)");

    // 2. Hoàn trả lượt sử dụng voucher nếu có
    if (order.getVoucher() != null) {
      voucherRepository.decrementUsedCountAtomic(order.getVoucher().getVoucherId());
    }

    // 3. Cập nhật Payment sang EXPIRED
    paymentRepository
        .findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId())
        .ifPresent(
            payment -> {
              payment.setStatus(PaymentStatus.EXPIRED.name());
              paymentRepository.save(payment);
            });

    // 4. Cập nhật Order sang CANCELLED
    order.setStatus(OrderStatus.CANCELLED.name());
    order.setPaymentStatus(PaymentStatus.EXPIRED.name());
    order.setCancelReason(
        "Mã QR hết hiệu lực thanh toán (Quá thời hạn giữ chỗ " + expireMins + " phút)");
    order.setUpdatedAt(now);
    orderRepository.save(order);

    // 5. Ghi lịch sử trạng thái
    orderStatusHistoryRepository.save(
        OrderStatusHistory.builder()
            .order(order)
            .changedBy(null)
            .previousStatus(currentStatus)
            .newStatus(OrderStatus.CANCELLED.name())
            .note("Hệ thống tự động hủy đơn do quá hạn thanh toán QR " + expireMins + " phút.")
            .build());

    // 6. Ghi Outbox Event OrderCancelled (NotificationEventListener tự gửi thông báo hủy)
    Map<String, Object> expPayload = new HashMap<>();
    expPayload.put("eventId", UUID.randomUUID().toString());
    expPayload.put("orderId", order.getOrderId());
    expPayload.put("orderCode", order.getOrderCode() != null ? order.getOrderCode() : "");
    expPayload.put("userId", order.getUser() != null ? order.getUser().getUserId() : null);
    expPayload.put(
        "cancelReason",
        "Mã QR hết hiệu lực thanh toán (Quá thời hạn giữ chỗ " + expireMins + " phút)");
    expPayload.put("refundPending", false);

    outboxService.saveEvent(
        "ORDER",
        order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId()),
        "OrderCancelled",
        KafkaTopics.ORDER_CANCELLED,
        expPayload);

    return true;
  }
}
