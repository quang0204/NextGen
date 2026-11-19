package com.wedbackendservice.nextgen.module.order.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.util.DateTimeUtil;
import com.wedbackendservice.nextgen.module.inventory.entity.Inventory;
import com.wedbackendservice.nextgen.module.inventory.entity.InventoryTransaction;
import com.wedbackendservice.nextgen.module.inventory.enums.InventoryTransactionType;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryTransactionRepository;
import com.wedbackendservice.nextgen.module.notification.entity.Notification;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.order.dto.client.request.BankInfoRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.request.CreateReturnRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.request.ReturnItemRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.response.CreateReturnResponse;
import com.wedbackendservice.nextgen.module.order.dto.client.response.OrderItemResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.request.InspectReturnRequest;
import com.wedbackendservice.nextgen.module.order.dto.portal.request.RestockItemRequest;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.AdminReturnItemResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.AdminReturnListResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.CustomerInfoResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.InspectReturnResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.InventoryRestockResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.RestockedItemResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.ReturnPaginationResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.ReturnSummaryResponse;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderItem;
import com.wedbackendservice.nextgen.module.order.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.module.order.entity.ReturnRequest;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.enums.ReturnStatus;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.order.repository.ReturnRequestRepository;
import com.wedbackendservice.nextgen.module.payment.entity.Payment;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentMethod;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import com.wedbackendservice.nextgen.module.product.entity.Product;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.product.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.voucher.repository.VoucherRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class ReturnsService {

  private final ReturnRequestRepository returnRequestRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final ProductVariantRepository productVariantRepository;
  private final PaymentRepository paymentRepository;
  private final AuditLogRepository auditLogRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final NotificationRepository notificationRepository;
  private final VoucherRepository voucherRepository;
  private final SecurityHelper securityHelper;
  private final ObjectMapper objectMapper;
  private final OutboxService outboxService;

  /** API 11.1: [POST] /api/v1/returns — Khách hàng tạo phiếu yêu cầu đổi trả (Authenticated) */
  @Transactional
  public CreateReturnResponse createReturn(CreateReturnRequest request) {
    User currentUser = securityHelper.getCurrentUser();

    Order order =
        orderRepository
            .findById(request.getOrderId())
            .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    // 1. Phải thuộc sở hữu của user đang đăng nhập
    if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
      throw new AppException(ErrorCode.RETURN_UNAUTHORIZED);
    }

    // 2. Đơn hàng ở trạng thái COMPLETED
    if (!OrderStatus.COMPLETED.name().equalsIgnoreCase(order.getStatus())) {
      throw new AppException(ErrorCode.RETURN_ORDER_NOT_COMPLETED);
    }

    // 3. Thời gian kể từ completed_at đến hiện tại <= 30 ngày
    LocalDateTime completedAt = order.getCompletedAt();
    if (completedAt == null) {
      completedAt = order.getUpdatedAt();
    }
    if (completedAt == null || completedAt.isBefore(LocalDateTime.now().minusDays(30))) {
      throw new AppException(ErrorCode.RETURN_PERIOD_EXPIRED);
    }

    // 4. Kiểm tra yêu cầu đổi trả trùng lặp (theo từng sản phẩm hoặc toàn bộ đơn)
    if (PaymentStatus.REFUNDED.name().equalsIgnoreCase(order.getPaymentStatus())) {
      throw new AppException(
          ErrorCode.DUPLICATE_RETURN_REQUEST,
          "Đơn hàng này đã được hoàn trả toàn bộ, không thể tạo thêm yêu cầu!");
    }

    boolean hasNewItems = request.getItems() != null && !request.getItems().isEmpty();
    if (!hasNewItems) {
      boolean hasExistingReturn =
          returnRequestRepository.existsByOrder_OrderIdAndStatusIn(
              order.getOrderId(),
              List.of(
                  ReturnStatus.PENDING.name(),
                  ReturnStatus.INSPECTING.name(),
                  ReturnStatus.APPROVED.name()));
      if (hasExistingReturn) {
        throw new AppException(ErrorCode.DUPLICATE_RETURN_REQUEST);
      }
    } else {
      List<ReturnRequest> existingReturns =
          returnRequestRepository
              .findByOrder_OrderIdInOrderByCreatedAtDesc(List.of(order.getOrderId()))
              .stream()
              .filter(
                  r ->
                      "PENDING".equalsIgnoreCase(r.getStatus())
                          || "INSPECTING".equalsIgnoreCase(r.getStatus())
                          || "APPROVED".equalsIgnoreCase(r.getStatus()))
              .toList();
      if (!existingReturns.isEmpty()) {
        for (ReturnItemRequest reqItem : request.getItems()) {
          for (ReturnRequest exRet : existingReturns) {
            if (isItemMatchingExisting(reqItem, exRet)) {
              if ("APPROVED".equalsIgnoreCase(exRet.getStatus())) {
                throw new AppException(
                    ErrorCode.DUPLICATE_RETURN_REQUEST,
                    "Sản phẩm "
                        + (reqItem.getProductName() != null ? reqItem.getProductName() : "")
                        + " đã được hoàn tiền thành công trước đó!");
              } else {
                throw new AppException(
                    ErrorCode.DUPLICATE_RETURN_REQUEST,
                    "Sản phẩm "
                        + (reqItem.getProductName() != null ? reqItem.getProductName() : "")
                        + " đang có yêu cầu đổi trả chờ xử lý!");
              }
            }
          }
        }
      }
    }

    // 5. Kiểm tra thông tin tài khoản ngân hàng nếu thanh toán ban đầu là COD
    boolean isCod =
        order.getPaymentMethod() != null
            && (order.getPaymentMethod().trim().equalsIgnoreCase("COD")
                || PaymentMethod.COD.name().equalsIgnoreCase(order.getPaymentMethod().trim()));
    if (isCod) {
      BankInfoRequest bankInfo = request.getBankInfo();
      if (bankInfo == null
          || !StringUtils.hasText(bankInfo.getBankName())
          || !StringUtils.hasText(bankInfo.getAccountNumber())
          || !StringUtils.hasText(bankInfo.getAccountHolder())) {
        throw new AppException(ErrorCode.RETURN_BANK_INFO_REQUIRED);
      }
    }

    String evidenceJson = null;
    if (request.getEvidenceImages() != null && !request.getEvidenceImages().isEmpty()) {
      try {
        evidenceJson = objectMapper.writeValueAsString(request.getEvidenceImages());
      } catch (Exception e) {
        evidenceJson = String.join(",", request.getEvidenceImages());
      }
    }

    String requestedItemsJson = null;
    if (request.getItems() != null && !request.getItems().isEmpty()) {
      try {
        requestedItemsJson = objectMapper.writeValueAsString(request.getItems());
      } catch (Exception e) {
        log.warn("Cannot serialize requestedItems: {}", e.getMessage());
      }
    }

    ReturnRequest returnRequest =
        ReturnRequest.builder()
            .order(order)
            .requestedBy(currentUser)
            .reason(request.getReason())
            .evidenceImages(evidenceJson)
            .requestedItems(requestedItemsJson)
            .status(ReturnStatus.PENDING.name())
            .bankName(request.getBankInfo() != null ? request.getBankInfo().getBankName() : null)
            .bankAccountNumber(
                request.getBankInfo() != null ? request.getBankInfo().getAccountNumber() : null)
            .bankAccountHolder(
                request.getBankInfo() != null ? request.getBankInfo().getAccountHolder() : null)
            .build();

    returnRequest = returnRequestRepository.save(returnRequest);

    auditLogRepository.log(
        currentUser,
        "CREATE_RETURN_REQUEST",
        "ReturnRequests",
        returnRequest.getReturnId(),
        null,
        "Tạo yêu cầu đổi trả cho đơn hàng #" + order.getOrderCode());

    return CreateReturnResponse.builder()
        .returnId(returnRequest.getReturnId())
        .orderId(order.getOrderId())
        .orderCode(order.getOrderCode())
        .requestedBy(currentUser.getUserId())
        .status(ReturnStatus.PENDING.name())
        .reason(returnRequest.getReason())
        .evidenceImages(request.getEvidenceImages())
        .createdAt(DateTimeUtil.formatIsoInstant(returnRequest.getCreatedAt()))
        .build();
  }

  /** API 11.2: [GET] /api/v1/admin/returns — Quản lý danh sách yêu cầu đổi trả (STAFF / ADMIN) */
  @Transactional(readOnly = true)
  public AdminReturnListResponse getAdminReturns(
      int page, int limit, String status, String orderCode, String sortBy, String sortDir) {
    securityHelper.authenticateStaffOrAdmin();

    int safePage = Math.max(page, 1) - 1;
    int safeLimit = (limit <= 0 || limit > 100) ? 20 : limit;

    String sortField = "status".equalsIgnoreCase(sortBy) ? "status" : "createdAt";
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable =
        PageRequest.of(
            safePage, safeLimit, Sort.by(direction, sortField).and(Sort.by(direction, "returnId")));

    Specification<ReturnRequest> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
            predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
          }
          if (StringUtils.hasText(orderCode)) {
            Join<ReturnRequest, Order> orderJoin = root.join("order", JoinType.LEFT);
            predicates.add(
                cb.like(
                    cb.lower(orderJoin.get("orderCode")),
                    "%" + orderCode.trim().toLowerCase() + "%"));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<ReturnRequest> pageResult = returnRequestRepository.findAll(spec, pageable);
    List<Long> orderIds =
        pageResult.getContent().stream()
            .map(ReturnRequest::getOrder)
            .filter(Objects::nonNull)
            .map(Order::getOrderId)
            .distinct()
            .toList();
    Map<Long, List<OrderItemResponse>> orderItemsByOrderId =
        orderIds.isEmpty()
            ? Map.of()
            : orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(
                    Collectors.groupingBy(
                        oi -> oi.getOrder().getOrderId(),
                        Collectors.mapping(
                            oi -> {
                              ProductVariant v = oi.getVariant();
                              Product p = v != null ? v.getProduct() : null;
                              return OrderItemResponse.builder()
                                  .orderItemId(oi.getOrderItemId())
                                  .variantId(v != null ? v.getVariantId() : null)
                                  .productId(p != null ? p.getProductId() : null)
                                  .productNameSnapshot(oi.getProductNameSnapshot())
                                  .skuSnapshot(oi.getSkuSnapshot())
                                  .colorSnapshot(oi.getColorSnapshot())
                                  .romSnapshot(oi.getRomSnapshot())
                                  .unitPrice(oi.getUnitPrice())
                                  .quantity(oi.getQuantity())
                                  .lineTotal(oi.getLineTotal())
                                  .imageUrl(oi.getImageUrl())
                                  .build();
                            },
                            Collectors.toList())));

    List<AdminReturnItemResponse> items =
        pageResult.getContent().stream()
            .map(
                r -> {
                  Order ord = r.getOrder();
                  User customer =
                      r.getRequestedBy() != null
                          ? r.getRequestedBy()
                          : (ord != null ? ord.getUser() : null);
                  CustomerInfoResponse customerInfo =
                      customer != null
                          ? CustomerInfoResponse.builder()
                              .userId(customer.getUserId())
                              .fullName(customer.getFullName())
                              .phone(customer.getPhone())
                              .build()
                          : null;
                  BankInfoRequest bankInfo =
                      (StringUtils.hasText(r.getBankName())
                              || StringUtils.hasText(r.getBankAccountNumber()))
                          ? BankInfoRequest.builder()
                              .bankName(r.getBankName())
                              .accountNumber(r.getBankAccountNumber())
                              .accountHolder(r.getBankAccountHolder())
                              .build()
                          : null;

                  List<OrderItemResponse> returnItems;
                  if (StringUtils.hasText(r.getRequestedItems())) {
                    try {
                      List<ReturnItemRequest> parsed =
                          objectMapper.readValue(
                              r.getRequestedItems(),
                              new com.fasterxml.jackson.core.type.TypeReference<
                                  List<ReturnItemRequest>>() {});
                      returnItems =
                          parsed.stream()
                              .map(
                                  it ->
                                      OrderItemResponse.builder()
                                          .variantId(it.getVariantId())
                                          .productNameSnapshot(it.getProductName())
                                          .skuSnapshot(it.getSku())
                                          .unitPrice(it.getUnitPrice())
                                          .quantity(it.getQuantity())
                                          .lineTotal(
                                              it.getUnitPrice() != null && it.getQuantity() != null
                                                  ? it.getUnitPrice()
                                                      .multiply(
                                                          BigDecimal.valueOf(it.getQuantity()))
                                                  : BigDecimal.ZERO)
                                          .build())
                              .toList();
                    } catch (Exception e) {
                      returnItems =
                          ord != null
                              ? orderItemsByOrderId.getOrDefault(ord.getOrderId(), List.of())
                              : List.of();
                    }
                  } else {
                    returnItems =
                        ord != null
                            ? orderItemsByOrderId.getOrDefault(ord.getOrderId(), List.of())
                            : List.of();
                  }

                  return AdminReturnItemResponse.builder()
                      .returnId(r.getReturnId())
                      .orderId(ord != null ? ord.getOrderId() : null)
                      .orderCode(ord != null ? ord.getOrderCode() : null)
                      .orderTotalAmount(ord != null ? ord.getFinalTotal() : null)
                      .customer(customerInfo)
                      .reason(r.getReason())
                      .evidenceImages(parseImages(r.getEvidenceImages()))
                      .status(r.getStatus())
                      .inspectionResult(r.getInspectionResult())
                      .inspectedBy(
                          r.getInspectedBy() != null ? resolveInspector(r.getInspectedBy()) : null)
                      .inspectedAt(DateTimeUtil.formatIsoInstant(r.getInspectedAt()))
                      .rejectReason(r.getRejectReason())
                      .bankInfo(bankInfo)
                      .items(returnItems)
                      .refundAmount(r.getRefundAmount())
                      .createdAt(DateTimeUtil.formatIsoInstant(r.getCreatedAt()))
                      .build();
                })
            .toList();

    long totalPending = 0;
    long totalApproved = 0;
    long totalRejected = 0;
    for (Object[] row : returnRequestRepository.countByStatusGrouped()) {
      if (row[0] != null && row[1] != null) {
        String st = ((String) row[0]).trim().toUpperCase();
        long cnt = ((Number) row[1]).longValue();
        if ("PENDING".equals(st) || "INSPECTING".equals(st)) {
          totalPending += cnt;
        } else if ("APPROVED".equals(st)) {
          totalApproved += cnt;
        } else if ("REJECTED".equals(st)) {
          totalRejected += cnt;
        }
      }
    }

    return AdminReturnListResponse.builder()
        .items(items)
        .summary(
            ReturnSummaryResponse.builder()
                .totalPending(totalPending)
                .totalApproved(totalApproved)
                .totalRejected(totalRejected)
                .build())
        .pagination(
            ReturnPaginationResponse.builder()
                .currentPage(page)
                .pageSize(safeLimit)
                .totalItems(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build())
        .build();
  }

  /**
   * API 11.3: [PUT] /api/v1/admin/returns/{id}/inspect — Biên bản kiểm định ngoại quan & Hoàn kho
   * MB04 (STAFF / ADMIN)
   */
  @Transactional(rollbackFor = Exception.class)
  public InspectReturnResponse inspectReturn(Long id, InspectReturnRequest request) {
    User currentUser = securityHelper.authenticateStaffOrAdmin();

    ReturnRequest returnRequest =
        returnRequestRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.RETURN_NOT_FOUND));

    // Kiểm tra xem yêu cầu đã thẩm định chưa
    if ("APPROVED".equalsIgnoreCase(returnRequest.getStatus())
        || "REJECTED".equalsIgnoreCase(returnRequest.getStatus())) {
      throw new AppException(ErrorCode.RETURN_ALREADY_PROCESSED);
    }

    String newStatus = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";
    if (!"INSPECTING".equals(newStatus)
        && !"APPROVED".equals(newStatus)
        && !"REJECTED".equals(newStatus)) {
      throw new AppException(ErrorCode.RETURN_INVALID_STATUS);
    }

    LocalDateTime now = LocalDateTime.now();
    String inspector = resolveInspector(currentUser);

    if ("INSPECTING".equals(newStatus)) {
      return handleInspecting(returnRequest, request, currentUser, now, inspector);
    }

    if ("REJECTED".equals(newStatus)) {
      return handleRejected(returnRequest, request, currentUser, now, inspector);
    }

    return handleApproved(returnRequest, request, currentUser, now, inspector);
  }

  private InspectReturnResponse handleInspecting(
      ReturnRequest returnRequest,
      InspectReturnRequest request,
      User currentUser,
      LocalDateTime now,
      String inspector) {
    returnRequest.setStatus("INSPECTING");
    returnRequest.setInspectionResult(request.getInspectionResult());
    returnRequest.setInspectedBy(currentUser);
    returnRequest.setInspectedAt(now);
    returnRequestRepository.saveAndFlush(returnRequest);

    auditLogRepository.log(
        currentUser,
        "RETURN_INSPECTING",
        "ReturnRequests",
        returnRequest.getReturnId(),
        "status: PENDING",
        "status: INSPECTING");

    return InspectReturnResponse.builder()
        .returnId(returnRequest.getReturnId())
        .orderId(returnRequest.getOrder() != null ? returnRequest.getOrder().getOrderId() : null)
        .orderCode(
            returnRequest.getOrder() != null ? returnRequest.getOrder().getOrderCode() : null)
        .status("INSPECTING")
        .inspectionResult(returnRequest.getInspectionResult())
        .inspectedBy(inspector)
        .inspectedAt(DateTimeUtil.formatIsoInstant(returnRequest.getInspectedAt()))
        .inventoryRestocked(false)
        .build();
  }

  private InspectReturnResponse handleRejected(
      ReturnRequest returnRequest,
      InspectReturnRequest request,
      User currentUser,
      LocalDateTime now,
      String inspector) {
    if (!StringUtils.hasText(request.getRejectReason())) {
      throw new AppException(ErrorCode.RETURN_REJECT_REASON_REQUIRED);
    }

    returnRequest.setStatus("REJECTED");
    returnRequest.setInspectionResult(request.getInspectionResult());
    returnRequest.setRejectReason(request.getRejectReason());
    returnRequest.setInspectedBy(currentUser);
    returnRequest.setInspectedAt(now);
    returnRequestRepository.saveAndFlush(returnRequest);

    String rejOrderCode =
        returnRequest.getOrder() != null
            ? returnRequest.getOrder().getOrderCode()
            : String.valueOf(returnRequest.getReturnId());
    recordCustomerReturnOutcome(
        returnRequest,
        returnRequest.getRequestedBy(),
        "Yêu cầu đổi trả đơn #" + rejOrderCode + " bị từ chối",
        "Yêu cầu đổi trả #RMA-"
            + returnRequest.getReturnId()
            + " bị từ chối: "
            + request.getRejectReason());

    auditLogRepository.log(
        currentUser,
        "RETURN_REJECTED",
        "ReturnRequests",
        returnRequest.getReturnId(),
        "status: PENDING",
        "status: REJECTED, reason: " + request.getRejectReason());

    return InspectReturnResponse.builder()
        .returnId(returnRequest.getReturnId())
        .orderId(returnRequest.getOrder() != null ? returnRequest.getOrder().getOrderId() : null)
        .status(ReturnStatus.REJECTED.name())
        .inspectionResult(returnRequest.getInspectionResult())
        .rejectReason(returnRequest.getRejectReason())
        .inspectedBy(inspector)
        .inspectedAt(DateTimeUtil.formatIsoInstant(returnRequest.getInspectedAt()))
        .inventoryRestocked(false)
        .build();
  }

  private void validateApproval(ReturnRequest returnRequest, InspectReturnRequest request) {
    if (request.getRefundAmount() == null
        || request.getRefundAmount().compareTo(BigDecimal.ZERO) < 0) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST, "Số tiền hoàn trả không hợp lệ khi chấp thuận đổi trả.");
    }
    Order order = returnRequest.getOrder();
    if (order != null && order.getFinalAmount() != null) {
      BigDecimal previouslyRefunded =
          returnRequestRepository
              .findByOrder_OrderIdInOrderByCreatedAtDesc(List.of(order.getOrderId()))
              .stream()
              .filter(
                  r ->
                      !r.getReturnId().equals(returnRequest.getReturnId())
                          && "APPROVED".equalsIgnoreCase(r.getStatus())
                          && r.getRefundAmount() != null)
              .map(ReturnRequest::getRefundAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal remainingRefundable = order.getFinalAmount().subtract(previouslyRefunded);
      if (remainingRefundable.compareTo(BigDecimal.ZERO) < 0) {
        remainingRefundable = BigDecimal.ZERO;
      }
      if (request.getRefundAmount().compareTo(remainingRefundable) > 0) {
        throw new AppException(
            ErrorCode.INVALID_REQUEST,
            String.format(
                "Số tiền hoàn trả (%s VND) vượt quá số tiền còn lại có thể hoàn của đơn hàng (%s VND)!",
                request.getRefundAmount(), remainingRefundable));
      }
    }
    if (order != null && request.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
      String pStatus =
          order.getPaymentStatus() != null ? order.getPaymentStatus().trim().toUpperCase() : "";
      boolean isOrderDelivered =
          "COMPLETED".equalsIgnoreCase(order.getStatus())
              || "DELIVERED".equalsIgnoreCase(order.getStatus());
      boolean isPaid =
          "PAID".equals(pStatus) || "PARTIALLY_REFUNDED".equals(pStatus) || isOrderDelivered;

      if (!isPaid) {
        throw new AppException(
            ErrorCode.INVALID_REQUEST,
            "Đơn hàng chưa được thanh toán ("
                + order.getPaymentStatus()
                + "), không thể hoàn tiền!");
      }
    }
    if (request.getRestockItems() == null || request.getRestockItems().isEmpty()) {
      throw new AppException(ErrorCode.RETURN_RESTOCK_ITEMS_REQUIRED);
    }

    // Validate restock items against original order items
    if (order != null) {
      List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
      Map<Long, Integer> orderVariantQtyMap =
          orderItems.stream()
              .filter(oi -> oi.getVariant() != null)
              .collect(
                  Collectors.toMap(
                      oi -> oi.getVariant().getVariantId(), OrderItem::getQuantity, Integer::sum));

      for (RestockItemRequest item : request.getRestockItems()) {
        if (item.getVariantId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
          throw new AppException(
              ErrorCode.INVALID_REQUEST, "Sản phẩm hoặc số lượng hoàn kho không hợp lệ.");
        }
        Integer maxAllowed = orderVariantQtyMap.get(item.getVariantId());
        if (maxAllowed == null) {
          throw new AppException(
              ErrorCode.INVALID_REQUEST,
              "Sản phẩm (variantId "
                  + item.getVariantId()
                  + ") không thuộc đơn hàng được đổi trả.");
        }
        if (item.getQuantity() > maxAllowed) {
          throw new AppException(
              ErrorCode.INVALID_REQUEST,
              "Số lượng hoàn kho vượt quá số lượng trong đơn hàng (tối đa: " + maxAllowed + ").");
        }
      }
    }
  }

  private List<RestockedItemResponse> executeRestockInventoryMB04(
      ReturnRequest returnRequest,
      List<RestockItemRequest> restockItems,
      User currentUser,
      LocalDateTime now) {
    List<RestockedItemResponse> restockedList = new ArrayList<>();
    for (RestockItemRequest item : restockItems) {
      ProductVariant variant =
          productVariantRepository
              .findById(item.getVariantId())
              .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));

      Inventory inventory =
          inventoryRepository
              .findWithLockByVariantId(variant.getVariantId())
              .orElseGet(
                  () ->
                      inventoryRepository.save(
                          Inventory.builder()
                              .variant(variant)
                              .quantity(0)
                              .reservedQuantity(0)
                              .minThreshold(5)
                              .build()));

      int qtyBefore = inventory.getOnHandQty();
      int qtyRestock = item.getQuantity();
      int qtyAfter = qtyBefore + qtyRestock;

      inventoryRepository.increaseQuantityAtomic(variant.getVariantId(), qtyRestock, now);
      inventory.setQuantity(qtyAfter);

      InventoryTransaction txn =
          InventoryTransaction.builder()
              .variant(variant)
              .order(returnRequest.getOrder())
              .performedBy(currentUser)
              .transactionType(InventoryTransactionType.RETURN.name())
              .formCode("MB04")
              .quantity(qtyRestock)
              .qtyBefore(qtyBefore)
              .qtyAfter(qtyAfter)
              .reason(
                  "Nhập hoàn kho theo biên bản MB04 - Yêu cầu đổi trả #"
                      + returnRequest.getReturnId())
              .build();
      inventoryTransactionRepository.save(txn);

      auditLogRepository.log(
          currentUser,
          "RETURN_RESTOCK_MB04",
          "Inventories",
          inventory.getInventoryId(),
          "on_hand_qty: " + qtyBefore,
          "on_hand_qty: " + qtyAfter + ", form_code: MB04");

      restockedList.add(
          RestockedItemResponse.builder()
              .variantId(variant.getVariantId())
              .sku(variant.getSku())
              .quantityRestocked(qtyRestock)
              .newOnHandQty(inventory.getOnHandQty())
              .newAvailableQty(inventory.getAvailableQty())
              .build());
    }
    return restockedList;
  }

  private void executePaymentRefund(
      ReturnRequest returnRequest,
      BigDecimal refundAmount,
      Order order,
      User currentUser,
      LocalDateTime now) {
    Payment refundPayment =
        Payment.builder()
            .order(order)
            .paymentMethod(
                order.getPaymentMethod() != null ? order.getPaymentMethod() : "BANK_TRANSFER")
            .paymentProvider("SYSTEM_REFUND")
            .amount(refundAmount)
            .status(PaymentStatus.REFUNDED.name())
            .transactionRef(
                "REFUND-MB04-" + returnRequest.getReturnId() + "-" + System.currentTimeMillis())
            .paidAt(now)
            .build();
    paymentRepository.save(refundPayment);

    BigDecimal totalRefunded =
        returnRequestRepository
            .findByOrder_OrderIdInOrderByCreatedAtDesc(List.of(order.getOrderId()))
            .stream()
            .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()) && r.getRefundAmount() != null)
            .map(ReturnRequest::getRefundAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(refundAmount);

    boolean isPartial =
        order.getFinalAmount() != null && totalRefunded.compareTo(order.getFinalAmount()) < 0;
    order.setPaymentStatus(isPartial ? "PARTIALLY_REFUNDED" : PaymentStatus.REFUNDED.name());
    order.setUpdatedAt(now);
    orderRepository.save(order);

    // Hoàn trả lại lượt dùng voucher cho khách hàng nếu đơn hàng được hoàn trả toàn bộ
    if (!isPartial && order.getVoucher() != null) {
      voucherRepository.decrementUsedCountAtomic(order.getVoucher().getVoucherId());
    }

    auditLogRepository.log(
        currentUser,
        "PAYMENT_REFUND",
        "Payments",
        refundPayment.getPaymentId(),
        null,
        (isPartial ? "Hoàn trả một phần " : "Hoàn trả ")
            + refundAmount
            + " VND cho yêu cầu đổi trả #"
            + returnRequest.getReturnId());
  }

  private InspectReturnResponse handleApproved(
      ReturnRequest returnRequest,
      InspectReturnRequest request,
      User currentUser,
      LocalDateTime now,
      String inspector) {
    validateApproval(returnRequest, request);

    returnRequest.setStatus(ReturnStatus.APPROVED.name());
    returnRequest.setInspectionResult(request.getInspectionResult());
    returnRequest.setRefundAmount(request.getRefundAmount());
    returnRequest.setInspectedBy(currentUser);
    returnRequest.setInspectedAt(now);
    returnRequest.setApprovedAt(now);

    String restockedJson = null;
    if (request.getRestockItems() != null && !request.getRestockItems().isEmpty()) {
      try {
        restockedJson = objectMapper.writeValueAsString(request.getRestockItems());
      } catch (Exception e) {
        log.warn("Cannot serialize restockItems: {}", e.getMessage());
      }
    }
    returnRequest.setRestockedItems(restockedJson);
    returnRequestRepository.saveAndFlush(returnRequest);

    List<RestockedItemResponse> restockedList =
        executeRestockInventoryMB04(returnRequest, request.getRestockItems(), currentUser, now);

    Order order = returnRequest.getOrder();
    if (order != null) {
      executePaymentRefund(returnRequest, request.getRefundAmount(), order, currentUser, now);
    }

    String appOrderCode =
        order != null ? order.getOrderCode() : String.valueOf(returnRequest.getReturnId());
    BigDecimal totalRefundedSummary =
        order != null
            ? returnRequestRepository
                .findByOrder_OrderIdInOrderByCreatedAtDesc(List.of(order.getOrderId()))
                .stream()
                .filter(
                    r -> "APPROVED".equalsIgnoreCase(r.getStatus()) && r.getRefundAmount() != null)
                .map(ReturnRequest::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(request.getRefundAmount())
            : request.getRefundAmount();
    boolean isPartialRefund =
        order != null
            && order.getFinalAmount() != null
            && totalRefundedSummary.compareTo(order.getFinalAmount()) < 0;

    recordCustomerReturnOutcome(
        returnRequest,
        returnRequest.getRequestedBy(),
        isPartialRefund
            ? "Đã duyệt hoàn tiền một phần đơn #" + appOrderCode
            : "Đã duyệt đổi trả & hoàn tiền đơn #" + appOrderCode,
        isPartialRefund
            ? "Yêu cầu đổi trả #RMA-"
                + returnRequest.getReturnId()
                + " được chấp thuận: hoàn "
                + request.getRefundAmount().stripTrailingZeros().toPlainString()
                + "đ cho sản phẩm trả và lập phiếu nhập kho MB04."
            : "Yêu cầu đổi trả #RMA-"
                + returnRequest.getReturnId()
                + " được chấp thuận: hoàn "
                + request.getRefundAmount().stripTrailingZeros().toPlainString()
                + "đ và lập phiếu nhập kho MB04.");

    auditLogRepository.log(
        currentUser,
        "RETURN_APPROVED",
        "ReturnRequests",
        returnRequest.getReturnId(),
        "status: PENDING",
        "status: APPROVED, refundAmount: " + request.getRefundAmount());

    // Ghi nhận sự kiện đổi trả & hoàn kho MB04 vào outbox_events
    outboxService.saveProcessedEvent(
        "RETURN",
        "RMA-" + returnRequest.getReturnId(),
        "ReturnApprovedMB04",
        "return-approved-topic",
        Map.of(
            "returnId",
            returnRequest.getReturnId(),
            "orderId",
            order != null ? order.getOrderId() : 0,
            "orderCode",
            order != null ? order.getOrderCode() : "",
            "refundAmount",
            request.getRefundAmount() != null ? request.getRefundAmount() : BigDecimal.ZERO,
            "decision",
            "APPROVED",
            "formCode",
            "MB04",
            "restockedItemsCount",
            restockedList.size()));

    return InspectReturnResponse.builder()
        .returnId(returnRequest.getReturnId())
        .orderId(order != null ? order.getOrderId() : null)
        .orderCode(order != null ? order.getOrderCode() : null)
        .status(ReturnStatus.APPROVED.name())
        .inspectionResult(returnRequest.getInspectionResult())
        .refundAmount(returnRequest.getRefundAmount())
        .inspectedBy(inspector)
        .inspectedAt(DateTimeUtil.formatIsoInstant(returnRequest.getInspectedAt()))
        .approvedAt(DateTimeUtil.formatIsoInstant(returnRequest.getApprovedAt()))
        .inventoryRestock(
            InventoryRestockResponse.builder()
                .formCode("MB04")
                .txnType(InventoryTransactionType.RETURN.name())
                .restockedItems(restockedList)
                .build())
        .build();
  }

  private void recordCustomerReturnOutcome(
      ReturnRequest returnRequest, User customer, String title, String content) {
    String safeTitle =
        title != null && title.length() > 240 ? title.substring(0, 237) + "..." : title;
    String safeContent =
        content != null && content.length() > 240 ? content.substring(0, 237) + "..." : content;
    Order order = returnRequest.getOrder();
    if (order != null) {
      orderStatusHistoryRepository.save(
          OrderStatusHistory.builder()
              .order(order)
              .changedBy(returnRequest.getInspectedBy())
              .previousStatus(order.getStatus())
              .newStatus(order.getStatus())
              .note(safeContent)
              .build());
    }
    User targetUser = customer != null ? customer : (order != null ? order.getUser() : null);
    if (targetUser != null) {
      notificationRepository.save(
          Notification.builder()
              .user(targetUser)
              .order(order)
              .title(safeTitle)
              .content(safeContent)
              .isRead(false)
              .build());
    }
  }

  private List<String> parseImages(String evidenceImages) {
    if (!StringUtils.hasText(evidenceImages)) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(evidenceImages, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      return Arrays.stream(evidenceImages.split(","))
          .map(String::trim)
          .filter(StringUtils::hasText)
          .toList();
    }
  }

  private String resolveInspector(User user) {
    if (user == null) {
      return "system";
    }
    if (StringUtils.hasText(user.getEmail())) {
      return user.getEmail();
    }
    if (StringUtils.hasText(user.getFullName())) {
      return user.getFullName();
    }
    return String.valueOf(user.getUserId());
  }

  private boolean isItemMatchingExisting(ReturnItemRequest newItem, ReturnRequest existingReturn) {
    // 1. Kiểm tra qua requestedItems JSON
    if (StringUtils.hasText(existingReturn.getRequestedItems())) {
      try {
        List<ReturnItemRequest> existingItems =
            objectMapper.readValue(
                existingReturn.getRequestedItems(),
                new TypeReference<List<ReturnItemRequest>>() {});
        for (ReturnItemRequest ex : existingItems) {
          if (newItem.getVariantId() != null
              && ex.getVariantId() != null
              && newItem.getVariantId().equals(ex.getVariantId())) {
            return true;
          }
          if (StringUtils.hasText(newItem.getSku())
              && StringUtils.hasText(ex.getSku())
              && newItem.getSku().trim().equalsIgnoreCase(ex.getSku().trim())) {
            return true;
          }
          if (StringUtils.hasText(newItem.getProductName())
              && StringUtils.hasText(ex.getProductName())) {
            String n1 = newItem.getProductName().trim().toLowerCase().split("\\(")[0].trim();
            String n2 = ex.getProductName().trim().toLowerCase().split("\\(")[0].trim();
            if (n1.equals(n2) || n1.contains(n2) || n2.contains(n1)) {
              return true;
            }
          }
        }
      } catch (Exception e) {
        log.warn(
            "Cannot parse existing requestedItems for returnId {}: {}",
            existingReturn.getReturnId(),
            e.getMessage());
      }
    }

    // 2. Kiểm tra qua reason string
    if (StringUtils.hasText(existingReturn.getReason())) {
      String reasonLower = existingReturn.getReason().toLowerCase();
      if (StringUtils.hasText(newItem.getSku())
          && reasonLower.contains(newItem.getSku().trim().toLowerCase())) {
        return true;
      }
      if (StringUtils.hasText(newItem.getProductName())) {
        String fullName = newItem.getProductName().trim().toLowerCase();
        String baseName = fullName.split("\\(")[0].trim();
        if (reasonLower.contains(fullName)
            || (baseName.length() >= 3 && reasonLower.contains(baseName))) {
          return true;
        }
      }
    }

    // 3. Nếu phiếu cũ không chỉ định sản phẩm cụ thể và không phải là hoàn trả một phần -> nghĩa là
    // hoàn toàn bộ đơn
    boolean isPartial =
        (StringUtils.hasText(existingReturn.getReason())
            && (existingReturn.getReason().toLowerCase().contains("hoàn trả một phần")
                || existingReturn.getReason().toLowerCase().contains("hoan tra mot phan")));
    if (!isPartial && !StringUtils.hasText(existingReturn.getRequestedItems())) {
      return true;
    }

    return false;
  }
}
