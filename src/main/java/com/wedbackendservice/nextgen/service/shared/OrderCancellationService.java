package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.request.order.CancelOrderRequest;
import com.wedbackendservice.nextgen.dto.client.response.order.CancelOrderResponse;
import com.wedbackendservice.nextgen.entity.Notification;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.OrderStatus;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.NotificationRepository;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.PaymentRepository;
import com.wedbackendservice.nextgen.repository.VoucherRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private final SecurityHelper securityHelper;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final VoucherRepository voucherRepository;
    private final NotificationRepository notificationRepository;
    private final InventoryService inventoryService;
    private final OrderStateService orderStateService;
    private final OrderExpirationService orderExpirationService;
    private final OutboxService outboxService;
    private final Clock clock;

    @Value("${order.payment.expire-minutes:30}")
    private int paymentExpireMinutes = 30;

    @Transactional
    public CancelOrderResponse cancelOrder(String orderCode, CancelOrderRequest request) {
        User currentUser = securityHelper.getCurrentUser();

        Order order = orderRepository.findByOrderCodeForUpdate(orderCode.trim())
                .or(() -> orderRepository.findByOrderCode(orderCode.trim()))
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng có mã " + orderCode + "!"));

        boolean isStaffOrAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.STAFF;
        if (!isStaffOrAdmin && !order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền hủy đơn hàng của người khác!");
        }

        String currentStatus = order.getStatus();
        if (!OrderStatus.WAITING_CONFIRMATION.name().equalsIgnoreCase(currentStatus)
                && !OrderStatus.PENDING_PAYMENT.name().equalsIgnoreCase(currentStatus)) {
            throw new AppException(ErrorCode.ERR_ORDER_CANNOT_CANCEL,
                    "Không thể hủy đơn hàng do đơn đã được xác nhận hoặc đang trong quá trình giao hàng!");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String reason = (request != null && org.springframework.util.StringUtils.hasText(request.getReason()))
                ? request.getReason().trim()
                : "Khách hàng yêu cầu hủy đơn";

        // Rollback giải phóng tồn giữ chỗ via InventoryService
        boolean stockReleased = inventoryService.releaseStockForOrder(order.getOrderId(), reason);

        // Hoàn trả lượt sử dụng voucher nếu có
        if (order.getVoucher() != null) {
            voucherRepository.decrementUsedCountAtomic(order.getVoucher().getVoucherId());
        }

        // Cập nhật trạng thái Payment sang CANCELLED hoặc REFUND_PENDING nếu đã thanh toán / khách báo đã chuyển
        boolean needRefund = "PAID".equalsIgnoreCase(order.getPaymentStatus())
                || PaymentStatus.SUCCESS.name().equalsIgnoreCase(order.getPaymentStatus())
                || "CUSTOMER_NOTIFIED".equalsIgnoreCase(order.getPaymentStatus())
                || PaymentStatus.CUSTOMER_NOTIFIED.name().equalsIgnoreCase(order.getPaymentStatus());
        paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId()).ifPresent(payment -> {
            if (needRefund) {
                payment.setStatus(PaymentStatus.REFUND_PENDING.name());
            } else if (PaymentStatus.PENDING.name().equalsIgnoreCase(payment.getStatus()) || PaymentStatus.UNPAID.name().equalsIgnoreCase(payment.getStatus())) {
                payment.setStatus(PaymentStatus.CANCELLED.name());
            }
            paymentRepository.save(payment);
        });

        // Cập nhật đơn hàng qua OrderStateService
        order.setCancelReason(reason);
        if (needRefund) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING.name());
        }
        orderStateService.transition(order, OrderStatus.CANCELLED, currentUser,
                "Hủy đơn hàng: " + reason + (needRefund ? " (Đã tạo yêu cầu hoàn tiền REFUND_PENDING)" : ""));
        orderRepository.save(order);

        // Phát Outbox Event OrderCancelled / RefundRequested
        Map<String, Object> cancelPayload = new HashMap<>();
        cancelPayload.put("orderId", order.getOrderId());
        cancelPayload.put("orderCode", order.getOrderCode() != null ? order.getOrderCode() : "");
        cancelPayload.put("cancelReason", reason);
        cancelPayload.put("refundPending", needRefund);
        cancelPayload.put("refundAmount", needRefund ? order.getFinalTotal() : BigDecimal.ZERO);
        cancelPayload.put("refundRequestedAt", DateTimeUtil.formatIsoInstant(now));
        cancelPayload.put("cancelledBy", currentUser.getEmail() != null ? currentUser.getEmail() : "USER");
        outboxService.saveEvent("ORDER",
                order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId()),
                needRefund ? "RefundRequested" : "OrderCancelled",
                "order-cancelled-topic", cancelPayload);

        // Gửi thông báo hủy đơn cho khách hàng
        if (order.getUser() != null) {
            try {
                notificationRepository.save(Notification.builder()
                        .user(order.getUser())
                        .order(order)
                        .title("Đơn hàng đã bị hủy")
                        .content("Đơn hàng " + order.getOrderCode() + " đã được hủy thành công. Lý do: " + reason)
                        .isRead(false)
                        .build());
            } catch (Exception notifEx) {
                log.warn("Lỗi gửi thông báo hủy đơn: {}", notifEx.getMessage());
            }
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
                boolean cancelled = orderExpirationService.cancelSingleExpiredOrder(order.getOrderId(), expireMins);
                if (cancelled) {
                    successCount++;
                }
            } catch (Exception ex) {
                log.error("Lỗi khi hủy đơn hàng hết hạn ID {}: {}", order.getOrderId(), ex.getMessage(), ex);
            }
        }
        return successCount;
    }
}
