package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.response.order.OrderDetailResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.ShipmentDetailResponse;
import com.wedbackendservice.nextgen.dto.portal.request.order.ConfirmOrderRequest;
import com.wedbackendservice.nextgen.dto.portal.request.order.PackExportOrderRequest;
import com.wedbackendservice.nextgen.dto.portal.response.order.ConfirmOrderResponse;
import com.wedbackendservice.nextgen.dto.portal.response.order.ExportedItemResponse;
import com.wedbackendservice.nextgen.dto.portal.response.order.PackExportOrderResponse;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.OrderStatus;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.port.out.inventory.InventoryPort;
import com.wedbackendservice.nextgen.port.out.notification.NotificationPort;
import com.wedbackendservice.nextgen.port.out.payment.PaymentInfoDto;
import com.wedbackendservice.nextgen.port.out.payment.PaymentPort;
import com.wedbackendservice.nextgen.port.out.shipment.CreateShipmentCommand;
import com.wedbackendservice.nextgen.port.out.shipment.ShipmentInfoDto;
import com.wedbackendservice.nextgen.port.out.shipment.ShipmentPort;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.util.DateTimeUtil;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {

  private final SecurityHelper securityHelper;
  private final OrderRepository orderRepository;
  private final ShipmentPort shipmentPort;
  private final PaymentPort paymentPort;
  private final NotificationPort notificationPort;
  private final AuditLogRepository auditLogRepository;
  private final OrderStateService orderStateService;
  private final InventoryPort inventoryPort;
  private final OrdersQueryService ordersQueryService;
  private final OutboxService outboxService;
  private final Clock clock;

  @Transactional
  public ConfirmOrderResponse confirmOrder(Long id, ConfirmOrderRequest request) {
    User operator = securityHelper.authenticateStaffOrAdmin();

    Order order =
        orderRepository
            .findByIdForUpdate(id)
            .or(() -> orderRepository.findById(id))
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + id + "!"));

    String currentStatus = order.getStatus();
    if (!OrderStatus.WAITING_CONFIRMATION.name().equalsIgnoreCase(currentStatus)) {
      throw new AppException(
          ErrorCode.ERR_ORDER_INVALID_STATE,
          "Chỉ có thể xác nhận đơn hàng đang ở trạng thái Chờ xác nhận (WAITING_CONFIRMATION)!");
    }

    LocalDateTime now = LocalDateTime.now(clock);

    String confirmNote =
        StringUtils.hasText(request.getNote())
            ? request.getNote()
            : "Duyệt xác nhận đơn hàng MB02 thành công và tạo vận đơn vận chuyển ("
                + request.getCarrier().trim()
                + ")";
    orderStateService.transition(order, OrderStatus.CONFIRMED, operator, confirmNote);
    orderRepository.save(order);

    // Sinh mã tracking vận đơn theo hãng vận chuyển: [PREFIX]-YYYYMMDD-OrderId
    String dateStr = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String carrierPrefix = resolveCarrierPrefix(request.getCarrier());
    String trackingCode = carrierPrefix + dateStr + order.getOrderId();

    BigDecimal codAmt =
        "COD".equalsIgnoreCase(order.getPaymentMethod()) ? order.getFinalTotal() : BigDecimal.ZERO;

    CreateShipmentCommand cmd =
        CreateShipmentCommand.builder()
            .order(order)
            .carrierName(request.getCarrier().trim())
            .trackingCode(trackingCode)
            .recipientName(order.getRecipientName())
            .recipientPhone(order.getRecipientPhone())
            .shippingAddress(order.getShippingAddress())
            .codAmount(codAmt)
            .notes(request.getNote())
            .updatedBy(operator)
            .build();
    ShipmentInfoDto savedShipment = shipmentPort.createShipment(cmd);

    // Ghi Audit Log
    auditLogRepository.log(
        operator,
        "CONFIRM_ORDER",
        "Orders",
        order.getOrderId(),
        "status: " + currentStatus,
        "status: CONFIRMED, tracking: " + trackingCode);

    // Ghi nhận sự kiện xác nhận đơn hàng vào outbox_events để publish Kafka
    Map<String, Object> confirmPayload = new HashMap<>();
    confirmPayload.put("orderId", order.getOrderId());
    confirmPayload.put("orderCode", order.getOrderCode() != null ? order.getOrderCode() : "");
    confirmPayload.put("trackingCode", trackingCode);
    confirmPayload.put("status", "CONFIRMED");
    confirmPayload.put("carrier", request.getCarrier().trim());
    confirmPayload.put(
        "confirmedBy",
        operator.getFullName() != null
            ? operator.getFullName()
            : (operator.getEmail() != null ? operator.getEmail() : "ADMIN"));

    outboxService.saveEvent(
        "ORDER",
        order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId()),
        "OrderConfirmed",
        "order-confirmed-topic",
        confirmPayload);

    // Gửi thông báo cho khách hàng via NotificationPort
    if (order.getUser() != null) {
      notificationPort.sendNotification(
          order.getUser().getUserId(),
          order.getOrderId(),
          "Đơn hàng đã được xác nhận",
          "Đơn hàng "
              + order.getOrderCode()
              + " đã được xác nhận (MB02) và đang được chuẩn bị để giao cho "
              + request.getCarrier().trim()
              + ".");
    }

    String confirmedByName =
        operator.getFullName() != null
            ? operator.getFullName()
                + " ("
                + (operator.getRole() != null ? operator.getRole().name() : "Nhân viên")
                + ")"
            : "Nhân viên";

    ShipmentDetailResponse shipmentDetail =
        ShipmentDetailResponse.builder()
            .shipmentId(savedShipment.getShipmentId())
            .trackingCode(savedShipment.getTrackingNumber())
            .carrier(savedShipment.getCarrierName())
            .status(savedShipment.getShippingStatus())
            .recipientName(savedShipment.getRecipientName())
            .recipientPhone(savedShipment.getRecipientPhone())
            .codAmount(savedShipment.getCodAmount())
            .build();

    return ConfirmOrderResponse.builder()
        .orderId(order.getOrderId())
        .orderCode(order.getOrderCode())
        .orderStatus("CONFIRMED")
        .formCode("MB02")
        .shipment(shipmentDetail)
        .confirmedBy(confirmedByName)
        .confirmedAt(DateTimeUtil.formatIsoInstant(now))
        .build();
  }

  @Transactional
  public OrderDetailResponse prepareOrder(Long id) {
    User operator = securityHelper.authenticateStaffOrAdmin();
    Order order =
        orderRepository
            .findByIdForUpdate(id)
            .or(() -> orderRepository.findById(id))
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + id + "!"));

    String currentStatus = order.getStatus();
    if (!OrderStatus.CONFIRMED.name().equalsIgnoreCase(currentStatus)) {
      throw new AppException(
          ErrorCode.ERR_ORDER_INVALID_STATE,
          "Chỉ có thể chuẩn bị hàng cho đơn hàng đang ở trạng thái Đã xác nhận (CONFIRMED)!");
    }

    orderStateService.transition(
        order,
        OrderStatus.PREPARING,
        operator,
        "Nhân viên kho bắt đầu đóng gói kiện hàng cho đơn " + order.getOrderCode());
    orderRepository.save(order);

    auditLogRepository.log(
        operator,
        "PREPARE_ORDER",
        "Orders",
        order.getOrderId(),
        "status: " + currentStatus,
        "status: PREPARING");

    return ordersQueryService.getOrderDetail(order.getOrderCode());
  }

  @Transactional
  public PackExportOrderResponse packAndExportOrder(Long id, PackExportOrderRequest request) {
    User operator = securityHelper.authenticateStaffOrAdmin();

    Order order =
        orderRepository
            .findByIdForUpdate(id)
            .or(() -> orderRepository.findById(id))
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + id + "!"));

    String currentStatus = order.getStatus();
    if (!OrderStatus.CONFIRMED.name().equalsIgnoreCase(currentStatus)
        && !OrderStatus.PREPARING.name().equalsIgnoreCase(currentStatus)
        && !OrderStatus.PICKED_UP.name().equalsIgnoreCase(currentStatus)) {
      throw new AppException(
          ErrorCode.ERR_ORDER_NOT_CONFIRMED,
          "Đơn hàng phải được duyệt xác nhận (CONFIRMED) trước khi thực hiện xuất kho MB03!");
    }

    LocalDateTime now = LocalDateTime.now(clock);

    // Xuất kho vật lý MB03 nguyên tử via InventoryPort
    List<ExportedItemResponse> exportedItems =
        inventoryPort.exportStockForOrderWithDetails(
            order.getOrderId(), operator, request.getExportNote());
    if (exportedItems == null || exportedItems.isEmpty()) {
      throw new AppException(
          ErrorCode.ERR_STOCK_INSUFFICIENT, "Xuất kho không thành công do thiếu tồn kho vật lý!");
    }

    // Cập nhật trạng thái đơn sang SHIPPING qua OrderStateService
    String exportHistoryNote =
        "Đóng gói & Xuất kho MB03, bàn giao đơn vị vận chuyển"
            + (StringUtils.hasText(request.getAssignedShipper())
                ? " (Tài xế: " + request.getAssignedShipper() + ")"
                : "");
    orderStateService.transition(order, OrderStatus.SHIPPING, operator, exportHistoryNote);
    orderRepository.save(order);

    // Cập nhật trạng thái vận đơn via ShipmentPort
    ShipmentInfoDto savedShipment =
        shipmentPort.markShipped(
            order.getOrderId(), request.getAssignedShipper(), request.getExportNote(), now);

    // Ghi Audit Log
    auditLogRepository.log(
        operator,
        "PACK_EXPORT_ORDER",
        "Orders",
        order.getOrderId(),
        "status: " + currentStatus,
        "status: SHIPPING, form: MB03");

    // Gửi thông báo đóng gói & xuất kho cho khách hàng via NotificationPort
    if (order.getUser() != null) {
      notificationPort.sendNotification(
          order.getUser().getUserId(),
          order.getOrderId(),
          "Đơn hàng đang được giao",
          "Kiện hàng "
              + order.getOrderCode()
              + " đã xuất kho (MB03) và đang trên đường giao đến bạn.");
    }

    String exportedByName =
        operator.getFullName() != null
            ? operator.getFullName()
                + " ("
                + (operator.getRole() != null ? operator.getRole().name() : "Thủ kho")
                + ")"
            : "Thủ kho";

    ShipmentDetailResponse shipmentDetail =
        ShipmentDetailResponse.builder()
            .shipmentId(savedShipment.getShipmentId())
            .trackingCode(savedShipment.getTrackingNumber())
            .carrier(savedShipment.getCarrierName())
            .status(savedShipment.getShippingStatus())
            .recipientName(savedShipment.getRecipientName())
            .recipientPhone(savedShipment.getRecipientPhone())
            .codAmount(savedShipment.getCodAmount())
            .build();

    return PackExportOrderResponse.builder()
        .orderId(order.getOrderId())
        .orderCode(order.getOrderCode())
        .orderStatus("SHIPPING")
        .formCode("MB03")
        .inventoryDeducted(true)
        .exportedItems(exportedItems)
        .shipment(shipmentDetail)
        .exportedBy(exportedByName)
        .exportedAt(DateTimeUtil.formatIsoInstant(now))
        .build();
  }

  @Transactional
  public OrderDetailResponse confirmOrderPayment(Long orderId, String note) {
    User operator = securityHelper.getCurrentUser();
    if (operator.getRole() != Role.ADMIN && operator.getRole() != Role.STAFF) {
      throw new AppException(
          ErrorCode.UNAUTHORIZED,
          "Chỉ quản trị viên hoặc nhân viên mới có quyền xác nhận thanh toán!");
    }

    Order order =
        orderRepository
            .findByIdForUpdate(orderId)
            .or(() -> orderRepository.findById(orderId))
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.ERR_ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng ID " + orderId + "!"));

    if (!OrderStatus.PENDING_PAYMENT.name().equalsIgnoreCase(order.getStatus())) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST,
          "Không thể xác nhận thanh toán vì đơn hàng đang ở trạng thái " + order.getStatus() + "!");
    }

    Optional<PaymentInfoDto> paymentOpt = paymentPort.getLatestPayment(orderId);
    if (paymentOpt.isPresent()
        && PaymentStatus.SUCCESS.name().equalsIgnoreCase(paymentOpt.get().getStatus())) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST,
          "Giao dịch thanh toán này đã được xác nhận thành công trước đó!");
    }

    LocalDateTime now = LocalDateTime.now(clock);
    order.setPaymentStatus("PAID");
    String noteText =
        StringUtils.hasText(note)
            ? note.trim()
            : "Nhân viên xác nhận đã nhận tiền chuyển khoản ngân hàng";
    orderStateService.transition(order, OrderStatus.WAITING_CONFIRMATION, operator, noteText);
    Order updated = orderRepository.save(order);

    String txRef =
        "TXN-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    paymentPort.confirmPaymentSuccess(
        updated.getOrderId(), updated.getFinalTotal(), txRef, now, "SIMULATED_QR", "BANK_TRANSFER");

    try {
      auditLogRepository.log(
          operator,
          "CONFIRM_PAYMENT",
          "Orders",
          orderId,
          "paymentStatus: UNPAID",
          "paymentStatus: PAID");
    } catch (Exception e) {
      log.warn("Không thể ghi audit log confirmOrderPayment: {}", e.getMessage());
    }

    outboxService.saveEvent(
        "PAYMENT",
        updated.getOrderCode(),
        "PaymentSucceeded",
        "payment-events-topic",
        Map.of(
            "orderId", updated.getOrderId(),
            "orderCode", updated.getOrderCode(),
            "amount", updated.getFinalTotal(),
            "transactionRef", txRef,
            "paymentMethod", "BANK_TRANSFER",
            "paidAt", DateTimeUtil.formatIsoInstant(now),
            "confirmedBy", operator.getEmail() != null ? operator.getEmail() : "STAFF"));

    if (updated.getUser() != null) {
      notificationPort.sendNotification(
          updated.getUser().getUserId(),
          updated.getOrderId(),
          "Thanh toán thành công",
          "Đơn hàng "
              + updated.getOrderCode()
              + " đã được xác nhận thanh toán thành công số tiền "
              + updated.getFinalTotal()
              + "đ.");
    }

    return ordersQueryService.getOrderDetail(updated.getOrderCode());
  }

  @Transactional
  public OrderDetailResponse customerNotifyPayment(Long orderId, String note) {
    User currentUser = securityHelper.getCurrentUser();
    Order order =
        orderRepository
            .findByIdForUpdate(orderId)
            .or(() -> orderRepository.findById(orderId))
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.ERR_ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng ID " + orderId + "!"));

    if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
      throw new AppException(
          ErrorCode.UNAUTHORIZED, "Chỉ chủ sở hữu đơn hàng mới có thể gửi thông báo chuyển khoản!");
    }

    if (!OrderStatus.PENDING_PAYMENT.name().equalsIgnoreCase(order.getStatus())
        || !"BANK_TRANSFER".equalsIgnoreCase(order.getPaymentMethod())) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST, "Đơn hàng không thuộc diện chờ chuyển khoản ngân hàng!");
    }

    if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
      order.setPaymentStatus("CUSTOMER_NOTIFIED");
      order.setUpdatedAt(LocalDateTime.now(clock));
      orderRepository.save(order);

      paymentPort.updatePaymentStatus(orderId, "CUSTOMER_NOTIFIED");
    }

    return ordersQueryService.getOrderDetail(order.getOrderCode());
  }

  private String resolveCarrierPrefix(String carrier) {
    if (!StringUtils.hasText(carrier)) return "GHN-";
    String c = carrier.trim().toUpperCase();
    if (c.contains("VIETTEL") || c.contains("VTP")) return "VTP-";
    if (c.contains("GHTK") || c.contains("TIẾT KIỆM") || c.contains("TIET KIEM")) return "GHTK-";
    if (c.contains("VNPOST") || c.contains("EMS")) return "VNPOST-";
    if (c.contains("J&T") || c.contains("JT")) return "JT-";
    return "GHN-";
  }
}
