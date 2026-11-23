package com.wedbackendservice.nextgen.module.shipment.service;

import com.wedbackendservice.nextgen.core.dto.PageResponse;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.util.PaginationUtil;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryTransactionRepository;
import com.wedbackendservice.nextgen.module.inventory.service.InventoryService;
import com.wedbackendservice.nextgen.module.notification.entity.Notification;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderItem;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.order.service.OrderStateService;
import com.wedbackendservice.nextgen.module.payment.entity.Payment;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.shipment.dto.client.response.ShipmentTimelineItem;
import com.wedbackendservice.nextgen.module.shipment.dto.client.response.ShipmentTrackItem;
import com.wedbackendservice.nextgen.module.shipment.dto.client.response.ShipmentTrackResponse;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.request.ShipmentFailDeliveryRequest;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.request.ShipmentStatusUpdateRequest;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.response.AdminShipmentItemResponse;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.response.ShipmentFailDeliveryResponse;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.response.ShipmentStatusSyncResponse;
import com.wedbackendservice.nextgen.module.shipment.entity.Shipment;
import com.wedbackendservice.nextgen.module.shipment.enums.ShipmentStatus;
import com.wedbackendservice.nextgen.module.shipment.mapper.ShipmentMapper;
import com.wedbackendservice.nextgen.module.shipment.repository.ShipmentRepository;
import com.wedbackendservice.nextgen.module.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentsService {

  private final SecurityHelper securityHelper;
  private final ShipmentRepository shipmentRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final PaymentRepository paymentRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final NotificationRepository notificationRepository;
  private final ShipmentMapper shipmentMapper;
  private final OutboxService outboxService;
  private final InventoryService inventoryService;
  private final OrderStateService orderStateService;

  /**
   * API 9.1: Khách hàng tra cứu hành trình vận chuyển (Public) Bảo vệ PII: Che tên người nhận, che
   * số điện thoại (chỉ để lại 3 số cuối), che địa chỉ chi tiết. Làm sạch timeline: không để lộ mã
   * kho nội bộ MB02/MB03 và ghi chú nội bộ.
   */
  public ShipmentTrackResponse trackShipment(String trackingCode) {
    if (trackingCode == null || trackingCode.trim().isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "Mã tra cứu không được để trống");
    }

    Shipment shipment =
        shipmentRepository
            .findByTrackingCodeOrOrderCode(trackingCode.trim())
            .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));

    Order order = shipment.getOrder();

    List<ShipmentTrackItem> items = new ArrayList<>();
    if (order != null) {
      List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
      for (OrderItem oi : orderItems) {
        ProductVariant variant = oi.getVariant();
        String productName =
            variant != null && variant.getProduct() != null
                ? variant.getProduct().getProductName()
                : "Sản phẩm";
        String color = variant != null ? variant.getColor() : null;

        items.add(
            ShipmentTrackItem.builder()
                .productName(productName)
                .color(color)
                .quantity(oi.getQuantity())
                .build());
      }
    }

    List<ShipmentTimelineItem> timeline = buildTimeline(shipment, order);
    timeline.sort(Comparator.comparing(ShipmentTimelineItem::getTime));

    return ShipmentTrackResponse.builder()
        .shipmentId(shipment.getShipmentId())
        .trackingCode(shipment.getTrackingNumber())
        .orderCode(order != null ? order.getOrderCode() : null)
        .carrier(shipment.getCarrierName())
        .status(shipment.getShippingStatus())
        .statusLabel(getStatusLabel(shipment.getShippingStatus()))
        .recipientName(order != null ? maskRecipientName(order.getRecipientName()) : null)
        .recipientPhone(order != null ? maskPhone(order.getRecipientPhone()) : null)
        .shippingAddress(order != null ? maskShippingAddress(order.getShippingAddress()) : null)
        .estimatedDeliveryDate(shipment.getEstimatedDelivery())
        .items(items)
        .timeline(timeline)
        .build();
  }

  /** API 9.2: Quản trị danh sách vận đơn (Admin Portal) (STAFF / ADMIN) */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  public PageResponse<AdminShipmentItemResponse> getAdminShipments(
      int page, int size, String carrier, String status, String search) {
    securityHelper.authenticateStaffOrAdmin();

    log.debug(
        "Get admin shipments: page={}, size={}, carrier={}, status={}, search={}",
        page,
        size,
        carrier,
        status,
        search);

    Pageable pageable =
        PaginationUtil.createPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Specification<Shipment> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();

          if (carrier != null && !carrier.isBlank() && !"ALL".equalsIgnoreCase(carrier.trim())) {
            predicates.add(
                cb.like(
                    cb.upper(root.get("carrierName")), "%" + carrier.trim().toUpperCase() + "%"));
          }

          if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            predicates.add(cb.equal(root.get("shippingStatus"), status.trim()));
          }

          if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            var orderJoin = root.join("order", jakarta.persistence.criteria.JoinType.LEFT);

            predicates.add(
                cb.or(
                    cb.like(cb.lower(root.get("trackingNumber")), pattern),
                    cb.like(cb.lower(orderJoin.get("orderCode")), pattern),
                    cb.like(cb.lower(orderJoin.get("recipientName")), pattern),
                    cb.like(orderJoin.get("recipientPhone"), pattern)));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<Shipment> shipmentPage = shipmentRepository.findAll(spec, pageable);

    if (shipmentPage.isEmpty()) {
      return PageResponse.from(shipmentPage, s -> null);
    }

    List<Long> orderIds =
        shipmentPage.getContent().stream()
            .map(Shipment::getOrder)
            .filter(Objects::nonNull)
            .map(Order::getOrderId)
            .distinct()
            .toList();

    Map<Long, String> paymentMethodMap = new HashMap<>();
    if (!orderIds.isEmpty()) {
      List<Payment> payments = paymentRepository.findByOrderIdsOrderByCreatedAtDesc(orderIds);
      for (Payment p : payments) {
        if (p.getOrder() != null && p.getOrder().getOrderId() != null) {
          paymentMethodMap.putIfAbsent(p.getOrder().getOrderId(), p.getPaymentMethod());
        }
      }
    }

    return PageResponse.from(
        shipmentPage,
        s -> {
          String payMethod = "COD";
          if (s.getOrder() != null) {
            if (s.getOrder().getPaymentMethod() != null && !s.getOrder().getPaymentMethod().isBlank()) {
              payMethod = s.getOrder().getPaymentMethod();
            } else {
              payMethod = paymentMethodMap.getOrDefault(s.getOrder().getOrderId(), "COD");
            }
          }
          return shipmentMapper.toAdminShipmentItem(s, payMethod);
        });
  }

  /**
   * API 9.3: Cập nhật trạng thái vận đơn & Tự động đồng bộ 2 chiều (STAFF / ADMIN) Kiểm tra State
   * Machine nghiêm ngặt: không nhảy lùi trạng thái, không ghi đè khi gọi DELIVERED lặp lại.
   */
  @Transactional
  public ShipmentStatusSyncResponse updateShipmentStatus(
      Long id, ShipmentStatusUpdateRequest request) {
    User operator = securityHelper.authenticateStaffOrAdmin();

    Shipment shipment =
        shipmentRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));

    String currentStatusStr = shipment.getShippingStatus();
    String newStatusStr = request.getStatus().trim();

    ShipmentStatus targetStatus;
    try {
      targetStatus = ShipmentStatus.valueOf(newStatusStr.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST, "Trạng thái vận chuyển không hợp lệ: " + newStatusStr);
    }

    ShipmentStatus currentStatus = null;
    if (currentStatusStr != null && !currentStatusStr.isBlank()) {
      try {
        currentStatus = ShipmentStatus.valueOf(currentStatusStr.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        log.debug(
            "Trạng thái vận đơn hiện tại không khớp enum ShipmentStatus: {}", currentStatusStr);
      }
    }

    validateStatusTransition(currentStatus, targetStatus, newStatusStr, request);
    updateShipmentTimestamps(shipment, targetStatus, operator);

    Order order = shipment.getOrder();
    String synchronizedOrderStatus = null;
    boolean isPaymentCompleted = false;

    if (order != null) {
      if (targetStatus == ShipmentStatus.PICKED
          || targetStatus == ShipmentStatus.IN_TRANSIT
          || targetStatus == ShipmentStatus.DELIVERED) {
        exportOrderStockIfNotExported(order, operator);
      }

      if (targetStatus == ShipmentStatus.PICKED) {
        synchronizedOrderStatus = "PICKED_UP";
        orderStateService.transition(
            order,
            OrderStatus.PICKED_UP,
            operator,
            "Đồng bộ trạng thái từ Vận chuyển: "
                + getStatusLabel(targetStatus.name())
                + (request.getNote() != null ? " (" + request.getNote() + ")" : ""));
        order.setStatus(synchronizedOrderStatus);
        orderRepository.save(order);
      } else if (targetStatus == ShipmentStatus.IN_TRANSIT) {
        synchronizedOrderStatus = "SHIPPING";
        orderStateService.transition(
            order,
            OrderStatus.SHIPPING,
            operator,
            "Đồng bộ trạng thái từ Vận chuyển: "
                + getStatusLabel(targetStatus.name())
                + (request.getNote() != null ? " (" + request.getNote() + ")" : ""));
        order.setStatus(synchronizedOrderStatus);
        orderRepository.save(order);
      } else if (targetStatus == ShipmentStatus.DELIVERED) {
        isPaymentCompleted =
            handleOrderDeliveredSync(order, shipment, request, operator, LocalDateTime.now());
        synchronizedOrderStatus = order.getStatus();
      } else if (targetStatus == ShipmentStatus.FAILED_DELIVERY) {
        handleOrderFailedDeliverySync(order, shipment, request, operator, id);
        synchronizedOrderStatus = order.getStatus();
      }

      sendShipmentNotification(order, targetStatus);
    }

    return ShipmentStatusSyncResponse.builder()
        .shipmentId(shipment.getShipmentId())
        .trackingCode(shipment.getTrackingNumber())
        .shipmentStatus(shipment.getShippingStatus())
        .synchronizedOrderId(order != null ? order.getOrderId() : null)
        .synchronizedOrderStatus(synchronizedOrderStatus)
        .isPaymentCompleted(isPaymentCompleted)
        .build();
  }

  private void validateStatusTransition(
      ShipmentStatus currentStatus,
      ShipmentStatus targetStatus,
      String newStatusStr,
      ShipmentStatusUpdateRequest request) {
    if (currentStatus != null) {
      if (currentStatus == targetStatus) {
        throw new AppException(
            ErrorCode.INVALID_REQUEST, "Vận đơn đã ở trạng thái " + newStatusStr + " trước đó.");
      }
      if (!currentStatus.canTransitionTo(targetStatus)) {
        log.warn("Không thể chuyển đổi trạng thái từ {} sang {}", currentStatus, targetStatus);
        throw new AppException(
            ErrorCode.INVALID_SHIPMENT_STATUS_TRANSITION,
            String.format(
                "Tiến trình vận đơn không được nhảy cóc hoặc lùi lại! Không thể chuyển từ '%s' sang '%s'.",
                getStatusLabel(currentStatus.name()), getStatusLabel(targetStatus.name())));
      }
    }
    if (targetStatus == ShipmentStatus.FAILED_DELIVERY
        && (request.getNote() == null || request.getNote().trim().isBlank())) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST,
          "Bắt buộc ghi nhận nguyên nhân khi giao hàng thất bại (sai địa chỉ, không nghe máy...).");
    }
  }

  private void updateShipmentTimestamps(
      Shipment shipment, ShipmentStatus targetStatus, User operator) {
    shipment.setShippingStatus(targetStatus.name());
    shipment.setUpdatedBy(operator);

    if (targetStatus == ShipmentStatus.PICKED || targetStatus == ShipmentStatus.IN_TRANSIT) {
      if (shipment.getShippedAt() == null) {
        shipment.setShippedAt(LocalDateTime.now());
      }
    } else if (targetStatus == ShipmentStatus.DELIVERED) {
      if (shipment.getDeliveredAt() == null) {
        shipment.setDeliveredAt(LocalDateTime.now());
      }
    }
    shipmentRepository.save(shipment);
  }

  private boolean handleOrderDeliveredSync(
      Order order,
      Shipment shipment,
      ShipmentStatusUpdateRequest request,
      User operator,
      LocalDateTime paidNow) {
    boolean isCod = "COD".equalsIgnoreCase(order.getPaymentMethod());
    boolean isAlreadyPaid = "PAID".equalsIgnoreCase(order.getPaymentStatus());
    boolean confirmPaid =
        isAlreadyPaid || (request.getCodCollected() == null || request.getCodCollected());
    boolean isPaymentCompleted = false;

    if (confirmPaid) {
      if (order.getCompletedAt() == null) {
        order.setCompletedAt(paidNow);
      }
      order.setPaymentStatus("PAID");

      Optional<Payment> paymentOpt =
          paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId());
      if (paymentOpt.isPresent()) {
        Payment payment = paymentOpt.get();
        payment.setStatus(PaymentStatus.SUCCESS.name());
        if (payment.getPaidAt() == null) {
          payment.setPaidAt(paidNow);
        }
        paymentRepository.save(payment);
      } else {
        paymentRepository.save(
            Payment.builder()
                .order(order)
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod() : "COD")
                .paymentProvider("COD_SHIPPER")
                .amount(order.getFinalTotal())
                .status(PaymentStatus.SUCCESS.name())
                .transactionRef("COD-" + order.getOrderCode())
                .paidAt(paidNow)
                .build());
      }
      isPaymentCompleted = true;
      orderStateService.transition(
          order,
          OrderStatus.COMPLETED,
          operator,
          "Giao hàng thành công" + (isCod ? " & Hoàn tất thu tiền COD" : ""));
      order.setStatus("COMPLETED");
    } else {
      order.setPaymentStatus("UNPAID");
      orderStateService.transition(
          order,
          OrderStatus.DELIVERED,
          operator,
          "Giao hàng thành công (Chờ kế toán xác nhận công nợ COD)");
      order.setStatus("DELIVERED");
    }
    order.setUpdatedAt(paidNow);
    orderRepository.save(order);

    Map<String, Object> payload = new HashMap<>();
    payload.put("orderId", order.getOrderId());
    payload.put("orderCode", order.getOrderCode() != null ? order.getOrderCode() : "");
    payload.put(
        "trackingCode", shipment.getTrackingNumber() != null ? shipment.getTrackingNumber() : "");
    payload.put("status", order.getStatus());
    payload.put(
        "paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus() : "UNPAID");
    payload.put(
        "totalAmount", order.getFinalTotal() != null ? order.getFinalTotal() : BigDecimal.ZERO);
    payload.put("deliveredAt", paidNow.toString());

    outboxService.saveProcessedEvent(
        "ORDER",
        order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId()),
        "OrderDelivered",
        "order-delivered-topic",
        payload);

    return isPaymentCompleted;
  }

  private void handleOrderFailedDeliverySync(
      Order order,
      Shipment shipment,
      ShipmentStatusUpdateRequest request,
      User operator,
      Long shipmentId) {
    String failReason =
        (request.getNote() != null && !request.getNote().trim().isBlank())
            ? request.getNote().trim()
            : "Giao hàng không thành công";
    order.setCancelReason("Giao hàng thất bại: " + failReason);
    shipment.setFailReason(failReason);
    if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
      order.setPaymentStatus("REFUND_PENDING");
    }
    orderStateService.transition(
        order, OrderStatus.RETURNING, operator, "Giao hàng thất bại - Đang hoàn về: " + failReason);
    order.setStatus(OrderStatus.RETURNING.name());
    order.setUpdatedAt(LocalDateTime.now());
    orderRepository.save(order);
    restockOrderInventory(order, operator, failReason);

    outboxService.saveProcessedEvent(
        "SHIPMENT",
        shipment.getTrackingNumber() != null
            ? shipment.getTrackingNumber()
            : String.valueOf(shipmentId),
        "DeliveryFailed",
        "shipment-failed-topic",
        Map.of(
            "shipmentId",
            shipmentId,
            "orderId",
            order.getOrderId(),
            "orderCode",
            order.getOrderCode(),
            "trackingCode",
            shipment.getTrackingNumber() != null ? shipment.getTrackingNumber() : "",
            "failReason",
            failReason,
            "status",
            "FAILED_DELIVERY"));
  }

  private void sendShipmentNotification(Order order, ShipmentStatus targetStatus) {
    if (order.getUser() == null) {
      return;
    }
    String notifTitle = null;
    String notifContent = null;
    if (targetStatus == ShipmentStatus.PICKED) {
      notifTitle = "Đơn vị vận chuyển đã lấy hàng";
      notifContent =
          "Đơn hàng "
              + order.getOrderCode()
              + " đã được đơn vị vận chuyển tiếp nhận và chuẩn bị trung chuyển.";
    } else if (targetStatus == ShipmentStatus.IN_TRANSIT) {
      notifTitle = "Đơn hàng đang trên đường giao";
      notifContent =
          "Đơn hàng " + order.getOrderCode() + " đang được nhân viên giao hàng vận chuyển đến bạn.";
    } else if (targetStatus == ShipmentStatus.DELIVERED) {
      notifTitle = "Giao hàng thành công";
      notifContent =
          "Đơn hàng "
              + order.getOrderCode()
              + " đã được giao thành công. Cảm ơn bạn đã mua sắm tại NextGen!";
    }

    if (notifTitle != null) {
      notificationRepository.save(
          Notification.builder()
              .user(order.getUser())
              .order(order)
              .title(notifTitle)
              .content(notifContent)
              .isRead(false)
              .build());
    }
  }

  /**
   * API 9.4: Xử lý giao hàng thất bại (Admin) Ngăn chặn bug double restock: kiểm tra xem đơn đã bị
   * hủy/giao thất bại trước đó chưa. Cập nhật kho bằng hàm UPDATE atomic. Xử lý hoàn tiền cho đơn
   * thanh toán online (REFUND_PENDING).
   */
  @Transactional
  public ShipmentFailDeliveryResponse failDelivery(Long id, ShipmentFailDeliveryRequest request) {
    User operator = securityHelper.authenticateStaffOrAdmin();

    if (request.getFailReason() == null || request.getFailReason().trim().isBlank()) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST,
          "Bắt buộc ghi nhận nguyên nhân giao hàng thất bại (ví dụ: sai địa chỉ, không nghe máy).");
    }

    Shipment shipment =
        shipmentRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));

    if ("DELIVERED".equalsIgnoreCase(shipment.getShippingStatus())) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST,
          "Vận đơn đã ở trạng thái DELIVERED, không thể chuyển sang giao thất bại.");
    }

    Order order = shipment.getOrder();
    if (order != null && "CANCELLED".equalsIgnoreCase(order.getStatus())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng liên kết đã bị hủy trước đó.");
    }

    // Chặn lỗi hoàn kho 2 lần (Double Restock)
    if ("FAILED_DELIVERY".equalsIgnoreCase(shipment.getShippingStatus())) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST, "Vận đơn này đã được xử lý giao hàng thất bại trước đó.");
    }

    ShipmentStatus currentStatus = null;
    if (shipment.getShippingStatus() != null && !shipment.getShippingStatus().isBlank()) {
      try {
        currentStatus = ShipmentStatus.valueOf(shipment.getShippingStatus().trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        log.debug(
            "Trạng thái vận đơn hiện tại không khớp enum ShipmentStatus: {}",
            shipment.getShippingStatus());
      }
    }

    if (currentStatus != null && !currentStatus.canTransitionTo(ShipmentStatus.FAILED_DELIVERY)) {
      log.warn("Không thể chuyển đổi trạng thái từ {} sang FAILED_DELIVERY", currentStatus);
      throw new AppException(
          ErrorCode.INVALID_SHIPMENT_STATUS_TRANSITION,
          String.format(
              "Vận đơn đang ở trạng thái '%s', chỉ khi đang giao hàng (IN_TRANSIT) mới có thể ghi nhận giao thất bại!",
              getStatusLabel(currentStatus.name())));
    }

    String reason = request.getFailReason().trim();

    shipment.setShippingStatus("FAILED_DELIVERY");
    shipment.setFailReason(reason);
    shipment.setUpdatedBy(operator);
    shipmentRepository.save(shipment);

    int restockedCount = 0;

    if (order != null) {
      order.setCancelReason("Giao hàng thất bại: " + reason);

      // Kiểm tra xử lý hoàn tiền nếu đơn hàng đã thanh toán online
      boolean needRefund = "PAID".equalsIgnoreCase(order.getPaymentStatus());
      if (needRefund) {
        order.setPaymentStatus("REFUND_PENDING");
      }
      orderStateService.transition(
          order,
          OrderStatus.RETURNING,
          operator,
          "Giao hàng thất bại - Đang hoàn về: "
              + reason
              + (needRefund ? " (Đã đánh dấu chờ hoàn tiền)" : ""));
      order.setStatus(OrderStatus.RETURNING.name());
      orderRepository.save(order);

      restockedCount = restockOrderInventory(order, operator, reason);

      if (order.getUser() != null) {
        notificationRepository.save(
            Notification.builder()
                .user(order.getUser())
                .order(order)
                .title("Đơn hàng đang hoàn về")
                .content(
                    "Đơn hàng "
                        + order.getOrderCode()
                        + " giao không thành công ("
                        + reason
                        + ") và đang được hoàn về kho.")
                .isRead(false)
                .build());
      }

      // Ghi nhận sự kiện giao hàng thất bại vào outbox_events
      outboxService.saveProcessedEvent(
          "SHIPMENT",
          shipment.getTrackingNumber() != null ? shipment.getTrackingNumber() : String.valueOf(id),
          "DeliveryFailed",
          "shipment-failed-topic",
          Map.of(
              "shipmentId",
              id,
              "orderId",
              order.getOrderId(),
              "orderCode",
              order.getOrderCode(),
              "trackingCode",
              shipment.getTrackingNumber() != null ? shipment.getTrackingNumber() : "",
              "failReason",
              reason,
              "status",
              "FAILED_DELIVERY"));
    }

    return ShipmentFailDeliveryResponse.builder()
        .shipmentId(id)
        .trackingCode(shipment.getTrackingNumber())
        .shipmentStatus("FAILED_DELIVERY")
        .failReason(reason)
        .orderStatus(OrderStatus.RETURNING.name())
        .inventoryRestocked(restockedCount > 0)
        .restockedItemsCount(restockedCount)
        .build();
  }

  public int restockOrderInventory(Order order, User operator, String reason) {
    if (order == null) return 0;
    int restockedCount = inventoryService.restockReturnOrder(order.getOrderId(), operator, reason);
    if (restockedCount > 0) {
      outboxService.saveProcessedEvent(
          "INVENTORY",
          order.getOrderCode(),
          "StockRestockedMB04",
          "inventory-return-topic",
          Map.of(
              "orderId",
              order.getOrderId(),
              "orderCode",
              order.getOrderCode(),
              "formCode",
              "MB04",
              "restockedItemsCount",
              restockedCount,
              "reason",
              reason != null ? reason : "Hoàn tồn kho MB04"));
    }
    return restockedCount;
  }

  // =========================================================================
  // PRIVATE HELPERS
  // =========================================================================

  private List<ShipmentTimelineItem> buildTimeline(Shipment shipment, Order order) {
    List<ShipmentTimelineItem> list = new ArrayList<>();

    if (order != null && order.getCreatedAt() != null) {
      list.add(
          ShipmentTimelineItem.builder()
              .time(order.getCreatedAt())
              .status("ORDER_PLACED")
              .description("Đơn hàng đã được tạo thành công.")
              .build());
    }

    if (shipment.getCreatedAt() != null) {
      list.add(
          ShipmentTimelineItem.builder()
              .time(shipment.getCreatedAt())
              .status("READY_TO_PICK")
              .description(
                  "Đơn hàng đã được xác nhận. Đang chuẩn bị đóng gói và chờ đối tác lấy hàng.")
              .build());
    }

    if (shipment.getShippedAt() != null) {
      list.add(
          ShipmentTimelineItem.builder()
              .time(shipment.getShippedAt())
              .status("PICKED")
              .description(
                  "Kiện hàng đã xuất kho. Đơn vị vận chuyển "
                      + (shipment.getCarrierName() != null ? shipment.getCarrierName() : "")
                      + " đã nhận kiện hàng.")
              .build());
    }

    if ("IN_TRANSIT".equalsIgnoreCase(shipment.getShippingStatus())
        || "DELIVERED".equalsIgnoreCase(shipment.getShippingStatus())) {
      LocalDateTime transitTime =
          shipment.getShippedAt() != null ? shipment.getShippedAt() : shipment.getCreatedAt();
      list.add(
          ShipmentTimelineItem.builder()
              .time(transitTime)
              .status("IN_TRANSIT")
              .description("Kiện hàng đang trong quá trình luân chuyển tới địa chỉ nhận.")
              .build());
    }

    if (shipment.getDeliveredAt() != null) {
      list.add(
          ShipmentTimelineItem.builder()
              .time(shipment.getDeliveredAt())
              .status("DELIVERED")
              .description("Kiện hàng đã được giao thành công tới người nhận.")
              .build());
    }

    if (shipment.getFailReason() != null) {
      list.add(
          ShipmentTimelineItem.builder()
              .time(shipment.getUpdatedAt())
              .status("FAILED_DELIVERY")
              .description("Giao hàng không thành công: " + shipment.getFailReason())
              .build());
    }

    return list;
  }

  public static String maskRecipientName(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return "***";
    }
    String[] parts = fullName.trim().split("\\s+");
    if (parts.length == 1) {
      return parts[0].charAt(0) + "***";
    }
    StringBuilder sb = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      sb.append(" ").append(parts[i].charAt(0)).append("***");
    }
    return sb.toString();
  }

  public static String maskPhone(String phone) {
    return com.wedbackendservice.nextgen.core.util.TextUtil.maskPhone(phone);
  }

  public static String maskShippingAddress(String address) {
    if (address == null || address.isBlank()) {
      return "***";
    }
    int commaIndex = address.indexOf(",");
    if (commaIndex > 0 && commaIndex < address.length() - 1) {
      return "***," + address.substring(commaIndex + 1);
    }
    return "*** " + (address.length() > 10 ? address.substring(address.length() - 10) : "");
  }

  public static String getStatusLabel(String status) {
    if (status == null) return "Không xác định";
    return switch (status.toUpperCase()) {
      case "READY_TO_PICK" -> "Chờ lấy hàng";
      case "PICKED" -> "Đã lấy hàng";
      case "IN_TRANSIT" -> "Đang vận chuyển";
      case "DELIVERED" -> "Giao hàng thành công";
      case "FAILED_DELIVERY" -> "Giao hàng thất bại";
      case "CANCELLED" -> "Đã hủy";
      default -> status;
    };
  }

  private void exportOrderStockIfNotExported(Order order, User operator) {
    inventoryService.exportStockForOrder(
        order.getOrderId(),
        operator,
        "Xuất kho tự động khi bàn giao vận chuyển đơn #" + order.getOrderCode());
  }
}
