package com.wedbackendservice.nextgen.module.order.service;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
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
public class OrderStateService {

  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final Clock clock;

  @Transactional
  public void transition(Order order, OrderStatus toStatus, User actor, String note) {
    if (order == null) {
      throw new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng!");
    }
    if (toStatus == null) {
      throw new AppException(
          ErrorCode.ERR_ORDER_INVALID_STATE, "Trạng thái đích không được để trống!");
    }

    OrderStatus currentStatus = OrderStatus.fromString(order.getStatus());
    if (currentStatus == null) {
      log.warn(
          "[OrderStateService] Đơn hàng #{} có trạng thái không xác định: {}",
          order.getOrderId(),
          order.getStatus());
      order.setStatus(toStatus.name());
      order.setUpdatedAt(LocalDateTime.now(clock));
      recordHistory(order, null, toStatus, actor, note);
      return;
    }

    if (currentStatus == toStatus) {
      log.info(
          "[OrderStateService] Đơn hàng #{} đã ở trạng thái {}, bỏ qua chuyển đổi",
          order.getOrderId(),
          toStatus);
      return;
    }

    if (!currentStatus.canTransitionTo(toStatus)) {
      log.error(
          "[OrderStateService] Chuyển trạng thái bất hợp lệ cho đơn hàng #{}: {} -> {}",
          order.getOrderId(),
          currentStatus,
          toStatus);
      throw new AppException(
          ErrorCode.ERR_ORDER_INVALID_STATE,
          String.format(
              "Thao tác không hợp lệ đối với trạng thái hiện tại của đơn hàng (%s -> %s)!",
              currentStatus, toStatus));
    }

    order.setStatus(toStatus.name());
    order.setUpdatedAt(LocalDateTime.now(clock));
    recordHistory(order, currentStatus, toStatus, actor, note);
  }

  private void recordHistory(
      Order order, OrderStatus from, OrderStatus to, User actor, String note) {
    try {
      orderStatusHistoryRepository.save(
          OrderStatusHistory.builder()
              .order(order)
              .changedBy(actor)
              .previousStatus(from != null ? from.name() : null)
              .newStatus(to.name())
              .note(note)
              .build());
    } catch (Exception ex) {
      log.warn("[OrderStateService] Ghi nhận lịch sử trạng thái thất bại: {}", ex.getMessage());
    }
  }
}
