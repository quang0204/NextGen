package com.wedbackendservice.nextgen.module.order.service;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.module.order.dto.client.response.OrderDetailResponse;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryPort;
import com.wedbackendservice.nextgen.module.order.port.out.payment.PaymentPort;
import com.wedbackendservice.nextgen.module.order.port.out.voucher.VoucherPort;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.user.entity.User;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReturnService {

  private final SecurityHelper securityHelper;
  private final OrderRepository orderRepository;
  private final PaymentPort paymentPort;
  private final VoucherPort voucherPort;
  private final InventoryPort inventoryPort;
  private final OrderStateService orderStateService;
  private final OrdersQueryService ordersQueryService;
  private final Clock clock;

  @Transactional
  public OrderDetailResponse receiveReturnOrder(Long orderId, String note) {
    User operator = securityHelper.authenticateStaffOrAdmin();
    Order order =
        orderRepository
            .findByIdForUpdate(orderId)
            .or(() -> orderRepository.findById(orderId))
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.ERR_ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng ID " + orderId + "!"));

    String currentStatus = order.getStatus();
    if (!OrderStatus.RETURNING.name().equalsIgnoreCase(currentStatus)) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST,
          "Chỉ có thể xác nhận nhận hàng hoàn cho đơn hàng đang ở trạng thái RETURNING (Bưu tá giao"
              + " không thành công)!");
    }

    LocalDateTime now = LocalDateTime.now(clock);

    // Kiểm tra và thực hiện hoàn kho nếu chưa hoàn (MB04) qua InventoryPort
    inventoryPort.restockReturnOrder(order.getOrderId(), operator, note);

    // Hoàn trả voucher nếu có via VoucherPort
    if (order.getVoucher() != null) {
      voucherPort.releaseVoucher(order.getVoucher().getVoucherId());
    }

    boolean needRefund =
        "PAID".equalsIgnoreCase(order.getPaymentStatus())
            || PaymentStatus.SUCCESS.name().equalsIgnoreCase(order.getPaymentStatus())
            || PaymentStatus.REFUND_PENDING.name().equalsIgnoreCase(order.getPaymentStatus());
    if (needRefund) {
      order.setPaymentStatus(PaymentStatus.REFUND_PENDING.name());
    } else {
      order.setPaymentStatus(PaymentStatus.CANCELLED.name());
    }

    String finalReason =
        (note != null && !note.trim().isBlank())
            ? note.trim()
            : (order.getCancelReason() != null
                ? order.getCancelReason()
                : "Bưu tá hoàn hàng về kho");
    order.setCancelReason(finalReason);
    orderStateService.transition(
        order,
        OrderStatus.CANCELLED,
        operator,
        "Xác nhận nhận hàng hoàn về kho (MB04): " + finalReason);
    Order saved = orderRepository.save(order);

    paymentPort.updatePaymentStatus(
        order.getOrderId(),
        needRefund ? PaymentStatus.REFUND_PENDING.name() : PaymentStatus.CANCELLED.name());

    return ordersQueryService.getOrderDetail(saved.getOrderCode());
  }
}
