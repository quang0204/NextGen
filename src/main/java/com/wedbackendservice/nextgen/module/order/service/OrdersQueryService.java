package com.wedbackendservice.nextgen.module.order.service;

import com.wedbackendservice.nextgen.core.dto.PageResponse;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.util.DateTimeUtil;
import com.wedbackendservice.nextgen.module.order.dto.client.response.MyOrderItemResponse;
import com.wedbackendservice.nextgen.module.order.dto.client.response.OrderDetailResponse;
import com.wedbackendservice.nextgen.module.order.dto.client.response.OrderItemResponse;
import com.wedbackendservice.nextgen.module.order.dto.client.response.OrderStatusHistoryResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.AdminOrderItemResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.AdminOrderListResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.PipelineSummaryResponse;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderItem;
import com.wedbackendservice.nextgen.module.order.entity.ReturnRequest;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.mapper.OrderMapper;
import com.wedbackendservice.nextgen.module.order.port.out.shipment.ShipmentInfoDto;
import com.wedbackendservice.nextgen.module.order.port.out.shipment.ShipmentPort;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.order.repository.ReturnRequestRepository;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdersQueryService {

  private final SecurityHelper securityHelper;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final ShipmentPort shipmentPort;
  private final ReturnRequestRepository returnRequestRepository;
  private final OrderMapper orderMapper;

  @Transactional(readOnly = true)
  public PageResponse<MyOrderItemResponse> getMyOrders(int page, int size, String status) {
    User currentUser = securityHelper.getCurrentUser();

    int safePage = Math.max(page, 1) - 1;
    int safeSize = (size <= 0 || size > 100) ? 5 : size;
    Pageable pageable =
        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    Specification<Order> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("user").get("userId"), currentUser.getUserId()));

          if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
            predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<Order> pageResult = orderRepository.findAll(spec, pageable);
    List<Order> orders = pageResult.getContent();
    List<Long> orderIds = orders.stream().map(Order::getOrderId).toList();
    Map<Long, List<OrderItemResponse>> itemsByOrderId =
        orderIds.isEmpty()
            ? Map.of()
            : orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(
                    Collectors.groupingBy(
                        oi -> oi.getOrder().getOrderId(),
                        Collectors.mapping(orderMapper::toOrderItemResponse, Collectors.toList())));
    Map<Long, List<OrderStatusHistoryResponse>> historiesByOrderId =
        orderIds.isEmpty()
            ? Map.of()
            : orderStatusHistoryRepository
                .findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds)
                .stream()
                .collect(
                    Collectors.groupingBy(
                        h -> h.getOrder().getOrderId(),
                        Collectors.mapping(
                            orderMapper::toOrderStatusHistoryResponse, Collectors.toList())));
    Map<Long, ShipmentInfoDto> myShipmentMap =
        orderIds.isEmpty() ? Map.of() : shipmentPort.getShipmentsByOrderIds(orderIds);
    Map<Long, ReturnRequest> myReturnMap =
        (orderIds.isEmpty() || returnRequestRepository == null)
            ? Map.of()
            : returnRequestRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds).stream()
                .collect(Collectors.toMap(r -> r.getOrder().getOrderId(), r -> r, (r1, r2) -> r1));

    List<MyOrderItemResponse> items =
        orders.stream()
            .map(
                order -> {
                  ShipmentInfoDto shipment = myShipmentMap.get(order.getOrderId());
                  ReturnRequest latestReturn = myReturnMap.get(order.getOrderId());
                  String payStatus = resolveDisplayPaymentStatus(order);

                  return MyOrderItemResponse.builder()
                      .orderId(order.getOrderId())
                      .orderCode(order.getOrderCode())
                      .status(order.getStatus())
                      .recipientName(order.getRecipientName())
                      .recipientPhone(order.getRecipientPhone())
                      .shippingAddress(order.getShippingAddress())
                      .subtotal(order.getSubtotal())
                      .voucherDiscount(order.getVoucherDiscount())
                      .shippingFee(
                          order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
                      .totalAmount(order.getFinalTotal())
                      .paymentMethod(order.getPaymentMethod())
                      .paymentStatus(payStatus)
                      .carrier(shipment != null ? shipment.getCarrierName() : null)
                      .trackingCode(shipment != null ? shipment.getTrackingNumber() : null)
                      .cancelReason(order.getCancelReason())
                      .returnId(latestReturn != null ? latestReturn.getReturnId() : null)
                      .returnStatus(latestReturn != null ? latestReturn.getStatus() : null)
                      .returnReason(latestReturn != null ? latestReturn.getReason() : null)
                      .returnRejectReason(
                          latestReturn != null ? latestReturn.getRejectReason() : null)
                      .returnInspectionResult(
                          latestReturn != null ? latestReturn.getInspectionResult() : null)
                      .returnRefundAmount(
                          latestReturn != null ? latestReturn.getRefundAmount() : null)
                      .createdAt(DateTimeUtil.formatIsoInstant(order.getCreatedAt()))
                      .updatedAt(DateTimeUtil.formatIsoInstant(order.getUpdatedAt()))
                      .completedAt(DateTimeUtil.formatIsoInstant(order.getCompletedAt()))
                      .items(itemsByOrderId.getOrDefault(order.getOrderId(), List.of()))
                      .statusHistory(historiesByOrderId.getOrDefault(order.getOrderId(), List.of()))
                      .build();
                })
            .toList();

    return PageResponse.<MyOrderItemResponse>builder()
        .content(items)
        .page(pageResult.getNumber() + 1)
        .size(pageResult.getSize())
        .totalElements(pageResult.getTotalElements())
        .totalPages(pageResult.getTotalPages())
        .hasNext(pageResult.hasNext())
        .build();
  }

  @Transactional(readOnly = true)
  public OrderDetailResponse getOrderDetail(String orderCode) {
    User currentUser = securityHelper.getCurrentUser();

    Order order =
        orderRepository
            .findByOrderCode(orderCode.trim())
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.ERR_ORDER_NOT_FOUND,
                        "Không tìm thấy đơn hàng có mã " + orderCode + "!"));

    boolean isStaffOrAdmin =
        currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.STAFF;
    if (!isStaffOrAdmin && !order.getUser().getUserId().equals(currentUser.getUserId())) {
      throw new AppException(
          ErrorCode.UNAUTHORIZED, "Bạn không có quyền truy cập vào thông tin đơn hàng này!");
    }

    List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
    List<OrderItemResponse> itemResponses =
        orderItems.stream().map(orderMapper::toOrderItemResponse).toList();

    List<OrderStatusHistoryResponse> historyResponses =
        orderStatusHistoryRepository
            .findByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId())
            .stream()
            .map(orderMapper::toOrderStatusHistoryResponse)
            .toList();

    Optional<ShipmentInfoDto> shipmentOpt = shipmentPort.getShipmentByOrderId(order.getOrderId());
    String carrier = shipmentOpt.map(ShipmentInfoDto::getCarrierName).orElse(null);
    String trackingCode = shipmentOpt.map(ShipmentInfoDto::getTrackingNumber).orElse(null);
    ReturnRequest latestReturn =
        (returnRequestRepository != null)
            ? returnRequestRepository
                .findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId())
                .orElse(null)
            : null;

    String detailPaymentStatus = resolveDisplayPaymentStatus(order);

    return OrderDetailResponse.builder()
        .orderId(order.getOrderId())
        .orderCode(order.getOrderCode())
        .status(order.getStatus())
        .recipientName(order.getRecipientName())
        .recipientPhone(order.getRecipientPhone())
        .shippingAddress(order.getShippingAddress())
        .paymentMethod(order.getPaymentMethod())
        .paymentStatus(detailPaymentStatus)
        .subtotal(order.getSubtotal())
        .voucherDiscount(order.getVoucherDiscount())
        .shippingFee(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
        .totalAmount(order.getFinalTotal())
        .carrier(carrier)
        .trackingCode(trackingCode)
        .cancelReason(order.getCancelReason())
        .returnId(latestReturn != null ? latestReturn.getReturnId() : null)
        .returnStatus(latestReturn != null ? latestReturn.getStatus() : null)
        .returnReason(latestReturn != null ? latestReturn.getReason() : null)
        .returnRejectReason(latestReturn != null ? latestReturn.getRejectReason() : null)
        .returnInspectionResult(latestReturn != null ? latestReturn.getInspectionResult() : null)
        .returnRefundAmount(latestReturn != null ? latestReturn.getRefundAmount() : null)
        .createdAt(DateTimeUtil.formatIsoInstant(order.getCreatedAt()))
        .updatedAt(DateTimeUtil.formatIsoInstant(order.getUpdatedAt()))
        .completedAt(DateTimeUtil.formatIsoInstant(order.getCompletedAt()))
        .items(itemResponses)
        .statusHistory(historyResponses)
        .build();
  }

  @Transactional(readOnly = true)
  public AdminOrderListResponse getAdminOrders(
      int page, int size, String status, String paymentStatus) {
    securityHelper.authenticateStaffOrAdmin();

    int safePage = Math.max(page, 1) - 1;
    int safeSize = (size <= 0 || size > 100) ? 10 : size;
    Pageable pageable =
        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    Specification<Order> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();

          if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
            predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
          }

          if (StringUtils.hasText(paymentStatus) && !paymentStatus.equalsIgnoreCase("ALL")) {
            predicates.add(
                cb.equal(cb.upper(root.get("paymentStatus")), paymentStatus.trim().toUpperCase()));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<Order> pageResult = orderRepository.findAll(spec, pageable);
    List<Order> orders = pageResult.getContent();
    List<Long> orderIds = orders.stream().map(Order::getOrderId).toList();

    Map<Long, List<OrderItem>> orderItemsMap =
        orderIds.isEmpty()
            ? Map.of()
            : orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(oi -> oi.getOrder().getOrderId()));

    Map<Long, ShipmentInfoDto> shipmentMap =
        orderIds.isEmpty() ? Map.of() : shipmentPort.getShipmentsByOrderIds(orderIds);
    Map<Long, List<OrderStatusHistoryResponse>> historiesMap =
        orderIds.isEmpty()
            ? Map.of()
            : orderStatusHistoryRepository
                .findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds)
                .stream()
                .collect(
                    Collectors.groupingBy(
                        h -> h.getOrder().getOrderId(),
                        Collectors.mapping(
                            orderMapper::toOrderStatusHistoryResponse, Collectors.toList())));

    Map<Long, ReturnRequest> adminReturnMap =
        (orderIds.isEmpty() || returnRequestRepository == null)
            ? Map.of()
            : returnRequestRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds).stream()
                .collect(Collectors.toMap(r -> r.getOrder().getOrderId(), r -> r, (r1, r2) -> r1));

    List<AdminOrderItemResponse> items =
        orders.stream()
            .map(
                order -> {
                  List<OrderItem> orderItems =
                      orderItemsMap.getOrDefault(order.getOrderId(), List.of());
                  String itemsSummary = buildItemsSummary(orderItems);
                  ShipmentInfoDto shipment = shipmentMap.get(order.getOrderId());
                  ReturnRequest latestReturn = adminReturnMap.get(order.getOrderId());

                  String adminPayStatus = resolveDisplayPaymentStatus(order);

                  return AdminOrderItemResponse.builder()
                      .orderId(order.getOrderId())
                      .orderCode(order.getOrderCode())
                      .recipientName(order.getRecipientName())
                      .recipientPhone(order.getRecipientPhone())
                      .shippingAddress(order.getShippingAddress())
                      .paymentMethod(order.getPaymentMethod())
                      .paymentStatus(adminPayStatus)
                      .subtotal(order.getSubtotal())
                      .voucherDiscount(order.getVoucherDiscount())
                      .shippingFee(
                          order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
                      .totalAmount(order.getFinalTotal())
                      .status(order.getStatus())
                      .carrier(shipment != null ? shipment.getCarrierName() : null)
                      .trackingCode(shipment != null ? shipment.getTrackingNumber() : null)
                      .cancelReason(order.getCancelReason())
                      .returnId(latestReturn != null ? latestReturn.getReturnId() : null)
                      .returnStatus(latestReturn != null ? latestReturn.getStatus() : null)
                      .returnReason(latestReturn != null ? latestReturn.getReason() : null)
                      .returnRejectReason(
                          latestReturn != null ? latestReturn.getRejectReason() : null)
                      .returnInspectionResult(
                          latestReturn != null ? latestReturn.getInspectionResult() : null)
                      .returnRefundAmount(
                          latestReturn != null ? latestReturn.getRefundAmount() : null)
                      .itemsSummary(itemsSummary)
                      .createdAt(DateTimeUtil.formatIsoInstant(order.getCreatedAt()))
                      .updatedAt(DateTimeUtil.formatIsoInstant(order.getUpdatedAt()))
                      .completedAt(DateTimeUtil.formatIsoInstant(order.getCompletedAt()))
                      .items(orderItems.stream().map(orderMapper::toOrderItemResponse).toList())
                      .statusHistory(historiesMap.getOrDefault(order.getOrderId(), List.of()))
                      .build();
                })
            .toList();

    Map<String, Long> statusCounts =
        orderRepository.countGroupByStatus().stream()
            .filter(r -> r != null && r[0] != null && r[1] != null)
            .collect(
                Collectors.toMap(
                    r -> (String) r[0], r -> ((Number) r[1]).longValue(), (a, b) -> a));

    PipelineSummaryResponse pipeline =
        PipelineSummaryResponse.builder()
            .pendingPayment(statusCounts.getOrDefault("PENDING_PAYMENT", 0L))
            .waitingConfirmation(statusCounts.getOrDefault("WAITING_CONFIRMATION", 0L))
            .confirmed(statusCounts.getOrDefault("CONFIRMED", 0L))
            .preparing(statusCounts.getOrDefault("PREPARING", 0L))
            .shipping(statusCounts.getOrDefault("SHIPPING", 0L))
            .completed(statusCounts.getOrDefault("COMPLETED", 0L))
            .cancelled(statusCounts.getOrDefault("CANCELLED", 0L))
            .build();

    return AdminOrderListResponse.builder()
        .content(items)
        .page(pageResult.getNumber() + 1)
        .size(pageResult.getSize())
        .totalElements(pageResult.getTotalElements())
        .totalPages(pageResult.getTotalPages())
        .hasNext(pageResult.hasNext())
        .pipelineSummary(pipeline)
        .build();
  }

  private String buildItemsSummary(List<OrderItem> items) {
    if (items == null || items.isEmpty()) {
      return "Không có sản phẩm";
    }
    OrderItem first = items.get(0);
    String firstName =
        first.getProductNameSnapshot() != null ? first.getProductNameSnapshot() : "Sản phẩm";
    int firstQty = first.getQuantity() != null ? first.getQuantity() : 1;

    if (items.size() == 1) {
      return String.format("%s (x%d)", firstName, firstQty);
    } else {
      return String.format("%s (x%d) + %d sản phẩm khác", firstName, firstQty, items.size() - 1);
    }
  }

  public String resolveDisplayPaymentStatus(Order order) {
    if (order == null) return PaymentStatus.UNPAID.name();
    String currentPayStatus = order.getPaymentStatus();
    if ("PAID".equalsIgnoreCase(currentPayStatus)
        || PaymentStatus.SUCCESS.name().equalsIgnoreCase(currentPayStatus)
        || (OrderStatus.COMPLETED.name().equalsIgnoreCase(order.getStatus())
            && !PaymentStatus.REFUNDED.name().equalsIgnoreCase(currentPayStatus)
            && !PaymentStatus.PARTIALLY_REFUNDED.name().equalsIgnoreCase(currentPayStatus))) {
      return "PAID";
    }
    return currentPayStatus != null ? currentPayStatus : PaymentStatus.UNPAID.name();
  }
}
