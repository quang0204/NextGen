package com.wedbackendservice.nextgen.module.order.service;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.util.DateTimeUtil;
import com.wedbackendservice.nextgen.module.order.dto.client.request.CancelOrderRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.response.CancelOrderResponse;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryPort;
import com.wedbackendservice.nextgen.module.order.port.out.notification.NotificationPort;
import com.wedbackendservice.nextgen.module.order.port.out.payment.PaymentPort;
import com.wedbackendservice.nextgen.module.order.port.out.voucher.VoucherPort;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancellationService {

  private final SecurityHelper securityHelper;
  private final OrderRepository orderRepository;
  private final PaymentPort paymentPort;
  private final VoucherPort voucherPort;
  private final NotificationPort notificationPort;
  private final InventoryPort inventoryPort;
  private final OrderStateService orderStateService;
  private final OrderExpirationService orderExpirationService;
  private final OutboxService outboxService;
  private final Clock clock;

  @Value("${order.payment.expire-minutes:30}")
  private int paymentExpireMinutes = 30;

  @Transactional
  public CancelOrderResponse cancelOrder(String orderCode, CancelOrderRequest request) {
    User currentUser = securityHelper.getCurrentUser();

    Order order =
        orderRepository
            .findByOrderCodeForUpdate(orderCode.trim())
            .or(() -> orderRepository.findByOrderCode(orderCode.trim()))
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.ERR_ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng có mã " + orderCode + "!"));

    boolean isStaffOrAdmin =
        currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.STAFF;
    if (!isStaffOrAdmin && !order.getUser().getUserId().equals(currentUser.getUserId())) {
      throw new AppException(
          ErrorCode.UNAUTHORIZED, "Bạn không có quyền hủy đơn hàng của người khác!");
    }

    String currentStatus = order.getStatus();
    if (!OrderStatus.WAITING_CONFIRMATION.name().equalsIgnoreCase(currentStatus)
        && !OrderStatus.PENDING_PAYMENT.name().equalsIgnoreCase(currentStatus)) {
      throw new AppException(
          ErrorCode.ERR_ORDER_CANNOT_CANCEL,
          "Không thể hủy đơn hàng do đơn đã được xác nhận hoặc đang trong quá trình giao hàng!");
    }

    LocalDateTime now = LocalDateTime.now(clock);
    String reason =
        (request != null && org.springframework.util.StringUtils.hasText(request.getReason()))
            ? request.getReason().trim()
            : "Khách hàng yêu cầu hủy đơn";

    // Rollback giải phóng tồn giữ chỗ via InventoryPort
    boolean stockReleased = inventoryPort.releaseStockForOrder(order.getOrderId(), reason);

    // Hoàn trả lượt sử dụng voucher nếu có via VoucherPort
    if (order.getVoucher() != null) {
      voucherPort.releaseVoucher(order.getVoucher().getVoucherId());
    }

    // Cập nhật trạng thái Payment sang CANCELLED hoặc REFUND_PENDING nếu đã thanh toán / khách báo
    // đã chuyển via PaymentPort
    boolean needRefund =
        "PAID".equalsIgnoreCase(order.getPaymentStatus())
            || PaymentStatus.SUCCESS.name().equalsIgnoreCase(order.getPaymentStatus())
            || "CUSTOMER_NOTIFIED".equalsIgnoreCase(order.getPaymentStatus())
            || PaymentStatus.CUSTOMER_NOTIFIED.name().equalsIgnoreCase(order.getPaymentStatus());

    if (needRefund) {
      paymentPort.updatePaymentStatus(order.getOrderId(), PaymentStatus.REFUND_PENDING.name());
    } else {
      paymentPort.updatePaymentStatus(order.getOrderId(), PaymentStatus.CANCELLED.name());
    }

    // Cập nhật đơn hàng qua OrderStateService
    order.setCancelReason(reason);
    if (needRefund) {
      order.setPaymentStatus(PaymentStatus.REFUND_PENDING.name());
    }
    orderStateService.transition(
        order,
        OrderStatus.CANCELLED,
        currentUser,
        "Hủy đơn hàng: "
            + reason
            + (needRefund ? " (Đã tạo yêu cầu hoàn tiền REFUND_PENDING)" : ""));
    orderRepository.save(order);

    // Phát Outbox Event OrderCancelled / RefundRequested
    Map<String, Object> cancelPayload = new HashMap<>();
    cancelPayload.put("orderId", order.getOrderId());
    cancelPayload.put("orderCode", order.getOrderCode() != null ? order.getOrderCode() : "");
    cancelPayload.put("cancelReason", reason);
    cancelPayload.put("refundPending", needRefund);
    cancelPayload.put("refundAmount", needRefund ? order.getFinalTotal() : BigDecimal.ZERO);
    cancelPayload.put("refundRequestedAt", DateTimeUtil.formatIsoInstant(now));
    cancelPayload.put(
        "cancelledBy", currentUser.getEmail() != null ? currentUser.getEmail() : "USER");
    outboxService.saveEvent(
        "ORDER",
        order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId()),
        needRefund ? "RefundRequested" : "OrderCancelled",
        "order-cancelled-topic",
        cancelPayload);

    // Gửi thông báo hủy đơn cho khách hàng via NotificationPort
    if (order.getUser() != null) {
      notificationPort.sendNotification(
          order.getUser().getUserId(),
          order.getOrderId(),
          "Đơn hàng đã bị hủy",
          "Đơn hàng " + order.getOrderCode() + " đã được hủy thành công. Lý do: " + reason);
    }

    return CancelOrderResponse.builder()
        .orderCode(order.getOrderCode())
        .status(OrderStatus.CANCELLED.name())
        .cancelReason(reason)
        .releasedHoldStock(stockReleased)
        .cancelledAt(DateTimeUtil.formatIsoInstant(now))
        .build();
  }

  public int cancelExpiredOrders() {
    int expireMins = paymentExpireMinutes > 0 ? paymentExpireMinutes : 30;
    LocalDateTime expiredBefore = LocalDateTime.now(clock).minusMinutes(expireMins);
    java.util.List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(expiredBefore);
    if (expiredOrders.isEmpty()) {
      return 0;
    }

    int successCount = 0;
    for (Order order : expiredOrders) {
      try {
        boolean cancelled =
            orderExpirationService.cancelSingleExpiredOrder(order.getOrderId(), expireMins);
        if (cancelled) {
          successCount++;
        }
      } catch (Exception ex) {
        log.error(
            "Lỗi khi hủy đơn hàng hết hạn ID {}: {}", order.getOrderId(), ex.getMessage(), ex);
      }
    }
    return successCount;
  }
}
