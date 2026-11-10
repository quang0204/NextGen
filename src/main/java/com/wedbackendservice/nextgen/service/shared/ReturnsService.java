package com.wedbackendservice.nextgen.service.shared;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.dto.client.request.returnrequest.BankInfoRequest;
import com.wedbackendservice.nextgen.dto.client.request.returnrequest.CreateReturnRequest;
import com.wedbackendservice.nextgen.dto.client.response.order.OrderItemResponse;
import com.wedbackendservice.nextgen.dto.client.response.returnrequest.CreateReturnResponse;
import com.wedbackendservice.nextgen.dto.portal.request.returnrequest.InspectReturnRequest;
import com.wedbackendservice.nextgen.dto.portal.request.returnrequest.RestockItemRequest;
import com.wedbackendservice.nextgen.dto.portal.response.returnrequest.*;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.PaymentMethod;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.util.DateTimeUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final SecurityHelper securityHelper;
    private final ObjectMapper objectMapper;

    /**
     * API 11.1: [POST] /api/v1/returns — Khách hàng tạo phiếu yêu cầu đổi trả (Authenticated)
     */
    @Transactional
    public CreateReturnResponse createReturn(CreateReturnRequest request) {
        User currentUser = securityHelper.getCurrentUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // 1. Phải thuộc sở hữu của user đang đăng nhập
        if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.RETURN_UNAUTHORIZED);
        }

        // 2. Đơn hàng ở trạng thái COMPLETED
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
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

        // 4. Chưa có yêu cầu đổi trả nào đang chờ duyệt (PENDING) hoặc đã được duyệt (APPROVED)
        boolean hasExistingReturn = returnRequestRepository.existsByOrder_OrderIdAndStatusIn(
                order.getOrderId(),
                List.of("PENDING", "INSPECTING", "APPROVED")
        );
        if (hasExistingReturn) {
            throw new AppException(ErrorCode.DUPLICATE_RETURN_REQUEST);
        }

        // 5. Kiểm tra thông tin tài khoản ngân hàng nếu thanh toán ban đầu là COD
        boolean isCod = order.getPaymentMethod() != null &&
                (order.getPaymentMethod().trim().equalsIgnoreCase("COD") ||
                 PaymentMethod.COD.name().equalsIgnoreCase(order.getPaymentMethod().trim()));
        if (isCod) {
            BankInfoRequest bankInfo = request.getBankInfo();
            if (bankInfo == null || !StringUtils.hasText(bankInfo.getBankName())
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

        ReturnRequest returnRequest = ReturnRequest.builder()
                .order(order)
                .requestedBy(currentUser)
                .reason(request.getReason())
                .evidenceImages(evidenceJson)
                .status("PENDING")
                .bankName(request.getBankInfo() != null ? request.getBankInfo().getBankName() : null)
                .bankAccountNumber(request.getBankInfo() != null ? request.getBankInfo().getAccountNumber() : null)
                .bankAccountHolder(request.getBankInfo() != null ? request.getBankInfo().getAccountHolder() : null)
                .build();

        returnRequest = returnRequestRepository.save(returnRequest);

        auditLogRepository.log(currentUser, "CREATE_RETURN_REQUEST", "ReturnRequests", returnRequest.getReturnId(),
                null, "Tạo yêu cầu đổi trả cho đơn hàng #" + order.getOrderCode());

        return CreateReturnResponse.builder()
                .returnId(returnRequest.getReturnId())
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .requestedBy(currentUser.getUserId())
                .status("PENDING")
                .reason(returnRequest.getReason())
                .evidenceImages(request.getEvidenceImages())
                .createdAt(DateTimeUtil.formatIsoInstant(returnRequest.getCreatedAt()))
                .build();
    }

    /**
     * API 11.2: [GET] /api/v1/admin/returns — Quản lý danh sách yêu cầu đổi trả (STAFF / ADMIN)
     */
    @Transactional(readOnly = true)
    public AdminReturnListResponse getAdminReturns(int page, int limit, String status, String orderCode,
                                                   String sortBy, String sortDir) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1) - 1;
        int safeLimit = (limit <= 0 || limit > 100) ? 20 : limit;

        String sortField = "status".equalsIgnoreCase(sortBy) ? "status" : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(safePage, safeLimit, Sort.by(direction, sortField).and(Sort.by(direction, "returnId")));

        Specification<ReturnRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (StringUtils.hasText(orderCode)) {
                Join<ReturnRequest, Order> orderJoin = root.join("order", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(orderJoin.get("orderCode")), "%" + orderCode.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ReturnRequest> pageResult = returnRequestRepository.findAll(spec, pageable);
        List<Long> orderIds = pageResult.getContent().stream()
                .map(ReturnRequest::getOrder)
                .filter(Objects::nonNull)
                .map(Order::getOrderId)
                .distinct()
                .toList();
        Map<Long, List<OrderItemResponse>> orderItemsByOrderId = orderIds.isEmpty() ? Map.of() :
                orderItemRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(
                                oi -> oi.getOrder().getOrderId(),
                                Collectors.mapping(oi -> {
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
                                }, Collectors.toList())
                        ));

        List<AdminReturnItemResponse> items = pageResult.getContent().stream().map(r -> {
            Order ord = r.getOrder();
            User customer = r.getRequestedBy() != null ? r.getRequestedBy() : (ord != null ? ord.getUser() : null);
            CustomerInfoResponse customerInfo = customer != null ? CustomerInfoResponse.builder()
                    .userId(customer.getUserId())
                    .fullName(customer.getFullName())
                    .phone(customer.getPhone())
                    .build() : null;
            BankInfoRequest bankInfo = (StringUtils.hasText(r.getBankName()) || StringUtils.hasText(r.getBankAccountNumber()))
                    ? BankInfoRequest.builder()
                            .bankName(r.getBankName())
                            .accountNumber(r.getBankAccountNumber())
                            .accountHolder(r.getBankAccountHolder())
                            .build()
                    : null;

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
                    .inspectedBy(r.getInspectedBy() != null ? resolveInspector(r.getInspectedBy()) : null)
                    .inspectedAt(DateTimeUtil.formatIsoInstant(r.getInspectedAt()))
                    .rejectReason(r.getRejectReason())
                    .bankInfo(bankInfo)
                    .items(ord != null ? orderItemsByOrderId.getOrDefault(ord.getOrderId(), List.of()) : List.of())
                    .refundAmount(r.getRefundAmount())
                    .createdAt(DateTimeUtil.formatIsoInstant(r.getCreatedAt()))
                    .build();
        }).toList();

        long totalPending = returnRequestRepository.countByStatusIn(List.of("PENDING", "INSPECTING"));
        long totalApproved = returnRequestRepository.countByStatus("APPROVED");
        long totalRejected = returnRequestRepository.countByStatus("REJECTED");

        return AdminReturnListResponse.builder()
                .items(items)
                .summary(ReturnSummaryResponse.builder()
                        .totalPending(totalPending)
                        .totalApproved(totalApproved)
                        .totalRejected(totalRejected)
                        .build())
                .pagination(ReturnPaginationResponse.builder()
                        .currentPage(page)
                        .pageSize(safeLimit)
                        .totalItems(pageResult.getTotalElements())
                        .totalPages(pageResult.getTotalPages())
                        .build())
                .build();
    }

    /**
     * API 11.3: [PUT] /api/v1/admin/returns/{id}/inspect — Biên bản kiểm định ngoại quan & Hoàn kho MB04 (STAFF / ADMIN)
     */
    @Transactional(rollbackFor = Exception.class)
    public InspectReturnResponse inspectReturn(Long id, InspectReturnRequest request) {
        User currentUser = securityHelper.authenticateStaffOrAdmin();

        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_NOT_FOUND));

        // Kiểm tra xem yêu cầu đã thẩm định chưa
        if ("APPROVED".equalsIgnoreCase(returnRequest.getStatus()) || "REJECTED".equalsIgnoreCase(returnRequest.getStatus())) {
            throw new AppException(ErrorCode.RETURN_ALREADY_PROCESSED);
        }

        String newStatus = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";
        if (!"INSPECTING".equals(newStatus) && !"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new AppException(ErrorCode.RETURN_INVALID_STATUS);
        }

        LocalDateTime now = LocalDateTime.now();
        String inspector = resolveInspector(currentUser);

        if ("INSPECTING".equals(newStatus)) {
            returnRequest.setStatus("INSPECTING");
            returnRequest.setInspectionResult(request.getInspectionResult());
            returnRequest.setInspectedBy(currentUser);
            returnRequest.setInspectedAt(now);
            returnRequestRepository.saveAndFlush(returnRequest);

            auditLogRepository.log(currentUser, "RETURN_INSPECTING", "ReturnRequests", returnRequest.getReturnId(),
                    "status: PENDING", "status: INSPECTING");

            return InspectReturnResponse.builder()
                    .returnId(returnRequest.getReturnId())
                    .orderId(returnRequest.getOrder() != null ? returnRequest.getOrder().getOrderId() : null)
                    .orderCode(returnRequest.getOrder() != null ? returnRequest.getOrder().getOrderCode() : null)
                    .status("INSPECTING")
                    .inspectionResult(returnRequest.getInspectionResult())
                    .inspectedBy(inspector)
                    .inspectedAt(DateTimeUtil.formatIsoInstant(returnRequest.getInspectedAt()))
                    .inventoryRestocked(false)
                    .build();
        }

        // Kịch bản TỪ CHỐI (REJECTED)
        if ("REJECTED".equals(newStatus)) {
            if (!StringUtils.hasText(request.getRejectReason())) {
                throw new AppException(ErrorCode.RETURN_REJECT_REASON_REQUIRED);
            }

            returnRequest.setStatus("REJECTED");
            returnRequest.setInspectionResult(request.getInspectionResult());
            returnRequest.setRejectReason(request.getRejectReason());
            returnRequest.setInspectedBy(currentUser);
            returnRequest.setInspectedAt(now);
            returnRequestRepository.saveAndFlush(returnRequest);

            String rejOrderCode = returnRequest.getOrder() != null ? returnRequest.getOrder().getOrderCode() : String.valueOf(returnRequest.getReturnId());
            recordCustomerReturnOutcome(
                    returnRequest,
                    returnRequest.getRequestedBy(),
                    "Yêu cầu đổi trả đơn #" + rejOrderCode + " bị từ chối",
                    "Yêu cầu đổi trả #RMA-" + returnRequest.getReturnId() + " bị từ chối: " + request.getRejectReason()
            );

            auditLogRepository.log(currentUser, "RETURN_REJECTED", "ReturnRequests", returnRequest.getReturnId(),
                    "status: PENDING", "status: REJECTED, reason: " + request.getRejectReason());

            return InspectReturnResponse.builder()
                    .returnId(returnRequest.getReturnId())
                    .orderId(returnRequest.getOrder() != null ? returnRequest.getOrder().getOrderId() : null)
                    .status("REJECTED")
                    .inspectionResult(returnRequest.getInspectionResult())
                    .rejectReason(returnRequest.getRejectReason())
                    .inspectedBy(inspector)
                    .inspectedAt(DateTimeUtil.formatIsoInstant(returnRequest.getInspectedAt()))
                    .inventoryRestocked(false)
                    .build();
        }

        // Kịch bản CHẤP THUẬN (APPROVED)
        if (request.getRefundAmount() == null || request.getRefundAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Số tiền hoàn trả không hợp lệ khi chấp thuận đổi trả.");
        }
        Order order = returnRequest.getOrder();
        if (order != null && order.getFinalAmount() != null && request.getRefundAmount().compareTo(order.getFinalAmount()) > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Số tiền hoàn trả không được vượt quá giá trị đơn hàng (" + order.getFinalAmount() + " VND).");
        }
        if (request.getRestockItems() == null || request.getRestockItems().isEmpty()) {
            throw new AppException(ErrorCode.RETURN_RESTOCK_ITEMS_REQUIRED);
        }

        // Validate restock items against original order items
        if (order != null) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
            Map<Long, Integer> orderVariantQtyMap = orderItems.stream()
                    .filter(oi -> oi.getVariant() != null)
                    .collect(Collectors.toMap(
                            oi -> oi.getVariant().getVariantId(),
                            OrderItem::getQuantity,
                            Integer::sum
                    ));

            for (RestockItemRequest item : request.getRestockItems()) {
                if (item.getVariantId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Sản phẩm hoặc số lượng hoàn kho không hợp lệ.");
                }
                Integer maxAllowed = orderVariantQtyMap.get(item.getVariantId());
                if (maxAllowed == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Sản phẩm (variantId " + item.getVariantId() + ") không thuộc đơn hàng được đổi trả.");
                }
                if (item.getQuantity() > maxAllowed) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Số lượng hoàn kho vượt quá số lượng trong đơn hàng (tối đa: " + maxAllowed + ").");
                }
            }
        }

        returnRequest.setStatus("APPROVED");
        returnRequest.setInspectionResult(request.getInspectionResult());
        returnRequest.setRefundAmount(request.getRefundAmount());
        returnRequest.setInspectedBy(currentUser);
        returnRequest.setInspectedAt(now);
        returnRequest.setApprovedAt(now);
        returnRequestRepository.saveAndFlush(returnRequest);

        // 1. Tự động hoàn kho vật lý: tăng on_hand_qty và available_qty trong Inventories (atomic)
        // 2. Ghi nhận sổ cái bất biến Inventory_Transactions với form_code = 'MB04' và txn_type = 'RETURN'
        List<RestockedItemResponse> restockedList = new ArrayList<>();
        for (RestockItemRequest item : request.getRestockItems()) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));

            Inventory inventory = inventoryRepository.findByVariant_VariantId(variant.getVariantId())
                    .orElseGet(() -> inventoryRepository.save(Inventory.builder()
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

            InventoryTransaction txn = InventoryTransaction.builder()
                    .variant(variant)
                    .order(returnRequest.getOrder())
                    .performedBy(currentUser)
                    .transactionType("RETURN")
                    .formCode("MB04")
                    .quantity(qtyRestock)
                    .qtyBefore(qtyBefore)
                    .qtyAfter(qtyAfter)
                    .reason("Nhập hoàn kho theo biên bản MB04 - Yêu cầu đổi trả #" + returnRequest.getReturnId())
                    .build();
            inventoryTransactionRepository.save(txn);

            auditLogRepository.log(currentUser, "RETURN_RESTOCK_MB04", "Inventories", inventory.getInventoryId(),
                    "on_hand_qty: " + qtyBefore, "on_hand_qty: " + qtyAfter + ", form_code: MB04");

            restockedList.add(RestockedItemResponse.builder()
                    .variantId(variant.getVariantId())
                    .sku(variant.getSku())
                    .quantityRestocked(qtyRestock)
                    .newOnHandQty(inventory.getOnHandQty())
                    .newAvailableQty(inventory.getAvailableQty())
                    .build());
        }

        // 3. Kích hoạt tiến trình hoàn tiền (refund_amount) qua phân hệ Payments
        if (order != null) {
            Payment refundPayment = Payment.builder()
                    .order(order)
                    .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod() : "BANK_TRANSFER")
                    .paymentProvider("SYSTEM_REFUND")
                    .amount(request.getRefundAmount())
                    .status(PaymentStatus.REFUNDED.name())
                    .transactionRef("REFUND-MB04-" + returnRequest.getReturnId() + "-" + System.currentTimeMillis())
                    .paidAt(now)
                    .build();
            paymentRepository.save(refundPayment);

            order.setPaymentStatus(PaymentStatus.REFUNDED.name());
            order.setUpdatedAt(now);
            orderRepository.save(order);

            auditLogRepository.log(currentUser, "PAYMENT_REFUND", "Payments", refundPayment.getPaymentId(),
                    null, "Hoàn trả " + request.getRefundAmount() + " VND cho yêu cầu đổi trả #" + returnRequest.getReturnId());
        }

        String appOrderCode = order != null ? order.getOrderCode() : String.valueOf(returnRequest.getReturnId());
        recordCustomerReturnOutcome(
                returnRequest,
                returnRequest.getRequestedBy(),
                "Đã duyệt đổi trả & hoàn tiền đơn #" + appOrderCode,
                "Yêu cầu đổi trả #RMA-" + returnRequest.getReturnId() + " được chấp thuận, hoàn " + request.getRefundAmount().stripTrailingZeros().toPlainString() + "đ và lập phiếu nhập kho MB04."
        );

        auditLogRepository.log(currentUser, "RETURN_APPROVED", "ReturnRequests", returnRequest.getReturnId(),
                "status: PENDING", "status: APPROVED, refundAmount: " + request.getRefundAmount());

        return InspectReturnResponse.builder()
                .returnId(returnRequest.getReturnId())
                .orderId(order != null ? order.getOrderId() : null)
                .orderCode(order != null ? order.getOrderCode() : null)
                .status("APPROVED")
                .inspectionResult(returnRequest.getInspectionResult())
                .refundAmount(returnRequest.getRefundAmount())
                .inspectedBy(inspector)
                .inspectedAt(DateTimeUtil.formatIsoInstant(returnRequest.getInspectedAt()))
                .approvedAt(DateTimeUtil.formatIsoInstant(returnRequest.getApprovedAt()))
                .inventoryRestock(InventoryRestockResponse.builder()
                        .formCode("MB04")
                        .txnType("RETURN")
                        .restockedItems(restockedList)
                        .build())
                .build();
    }

    private void recordCustomerReturnOutcome(ReturnRequest returnRequest, User customer, String title, String content) {
        String safeTitle = title != null && title.length() > 240 ? title.substring(0, 237) + "..." : title;
        String safeContent = content != null && content.length() > 240 ? content.substring(0, 237) + "..." : content;
        Order order = returnRequest.getOrder();
        if (order != null) {
            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .changedBy(returnRequest.getInspectedBy())
                    .previousStatus(order.getStatus())
                    .newStatus(order.getStatus())
                    .note(safeContent)
                    .build());
        }
        User targetUser = customer != null ? customer : (order != null ? order.getUser() : null);
        if (targetUser != null) {
            notificationRepository.save(Notification.builder()
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
}
