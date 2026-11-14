package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.util.DateTimeUtil;

import com.wedbackendservice.nextgen.dto.portal.request.order.*;
import com.wedbackendservice.nextgen.dto.client.request.order.*;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
import com.wedbackendservice.nextgen.dto.portal.response.order.*;
import com.wedbackendservice.nextgen.dto.client.response.order.*;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.PaymentMethod;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdersService {

    private final SecurityHelper securityHelper;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final VoucherRepository voucherRepository;
    private final PromotionVariantRepository promotionVariantRepository;
    private final ShipmentRepository shipmentRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final TokenRedisService tokenRedisService;
    private final PaymentRepository paymentRepository;
    @org.springframework.beans.factory.annotation.Value("${payment.bank-transfer.expire-minutes:30}")
    private int paymentExpireMinutes = 30;
    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("#,###");

    /**
     * API 8.1: [POST] /api/v1/orders — Đặt hàng Checkout (Khởi tạo MB01 & Giữ chỗ tồn kho) (Authenticated)
     */
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        User currentUser = securityHelper.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        // Kiểm tra Idempotency key nếu client gửi
        if (StringUtils.hasText(idempotencyKey)) {
            String redisKey = "idempotency:order:" + currentUser.getUserId() + ":" + idempotencyKey.trim();
            String existingOrderCode = tokenRedisService.getValue(redisKey);
            if (existingOrderCode != null) {
                Order existing = orderRepository.findByOrderCode(existingOrderCode).orElse(null);
                if (existing != null) {
                    List<OrderItem> existingItems = orderItemRepository.findByOrderId(existing.getOrderId());
                    return CreateOrderResponse.builder()
                            .orderId(existing.getOrderId())
                            .orderCode(existing.getOrderCode())
                            .status(existing.getStatus())
                            .subtotal(existing.getSubtotal())
                            .voucherDiscount(existing.getVoucherDiscount())
                            .shippingFee(existing.getShippingFee() != null ? existing.getShippingFee() : BigDecimal.ZERO)
                            .totalAmount(existing.getFinalTotal())
                            .paymentMethod(existing.getPaymentMethod())
                            .paymentStatus(existing.getPaymentStatus())
                            .itemsCount(existingItems != null ? existingItems.size() : 0)
                            .createdAt(DateTimeUtil.formatIsoInstant(existing.getCreatedAt()))
                            .build();
                }
            }
        }

        // 1. Gộp các sản phẩm trùng lặp và kiểm tra tồn khả dụng
        Map<Long, Integer> consolidatedQty = new LinkedHashMap<>();
        if (request.getItems() != null) {
            for (OrderItemRequest itemReq : request.getItems()) {
                if (itemReq.getVariantId() != null && itemReq.getQuantity() != null && itemReq.getQuantity() > 0) {
                    consolidatedQty.merge(itemReq.getVariantId(), itemReq.getQuantity(), Integer::sum);
                }
            }
        }
        if (consolidatedQty.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sách sản phẩm không hợp lệ!");
        }

        Map<ProductVariant, Integer> itemMap = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : consolidatedQty.entrySet()) {
            Long variantId = entry.getKey();
            int requestedQty = entry.getValue();

            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new AppException(ErrorCode.ERR_VARIANT_NOT_FOUND, "Không tìm thấy biến thể sản phẩm ID " + variantId + "!"));

            if (variant.getStatus() != null && !"ACTIVE".equalsIgnoreCase(variant.getStatus())) {
                String prodName = variant.getProduct() != null ? variant.getProduct().getProductName() : variant.getVariantName();
                throw new AppException(ErrorCode.PRODUCT_INACTIVE,
                        String.format("Biến thể '%s' (SKU: %s) hiện đã ngừng kinh doanh!", prodName, variant.getSku()));
            }

            if (variant.getProduct() != null && variant.getProduct().getStatus() != null && !"ACTIVE".equalsIgnoreCase(variant.getProduct().getStatus())) {
                throw new AppException(ErrorCode.PRODUCT_INACTIVE,
                        String.format("Sản phẩm '%s' hiện đã ngừng kinh doanh!", variant.getProduct().getProductName()));
            }

            Inventory inv = inventoryRepository.findByVariant_VariantId(variantId)
                    .orElseGet(() -> Inventory.builder().variant(variant).quantity(0).reservedQuantity(0).minThreshold(5).build());

            int available = inv.getAvailableQty();
            if (available < requestedQty) {
                String prodName = variant.getProduct() != null ? variant.getProduct().getProductName() : variant.getVariantName();
                throw new AppException(ErrorCode.ERR_STOCK_INSUFFICIENT,
                        String.format("Sản phẩm %s (SKU: %s) chỉ còn %d sản phẩm khả dụng trong kho!", prodName, variant.getSku(), available));
            }

            itemMap.put(variant, requestedQty);
        }

        // 2. Tính tiền (áp dụng Flash Sale nếu có)
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItemsToSave = new ArrayList<>();

        for (Map.Entry<ProductVariant, Integer> entry : itemMap.entrySet()) {
            ProductVariant variant = entry.getKey();
            int qty = entry.getValue();

            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;

            // Kiểm tra xem có chương trình Flash Sale nào đang ACTIVE cho variant này không
            List<PromotionVariant> activePromos = promotionVariantRepository.findActivePromotionsForVariant(variant.getVariantId(), now);
            if (activePromos != null && !activePromos.isEmpty() && activePromos.get(0).getPromotionalPrice() != null) {
                BigDecimal promoPrice = activePromos.get(0).getPromotionalPrice();
                if (promoPrice.compareTo(unitPrice) < 0) {
                    unitPrice = promoPrice;
                }
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            subtotal = subtotal.add(lineTotal);

            Product prod = variant.getProduct();
            String prodName = prod != null 
                    ? prod.getProductName() + (variant.getVariantName() != null && !variant.getVariantName().isBlank() ? " (" + variant.getVariantName() + ")" : "")
                    : variant.getVariantName();
            String rom = variant.getRomGb() != null ? variant.getRomGb() + "GB" : null;

            OrderItem orderItem = OrderItem.builder()
                    .variant(variant)
                    .productNameSnapshot(prodName)
                    .skuSnapshot(variant.getSku())
                    .colorSnapshot(variant.getColor())
                    .romSnapshot(rom)
                    .imageUrl(variant.getImageUrl())
                    .price(unitPrice)
                    .quantity(qty)
                    .totalPrice(lineTotal)
                    .build();

            orderItemsToSave.add(orderItem);
        }

        // 3. Áp dụng mã Voucher (nếu có)
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;

        if (StringUtils.hasText(request.getVoucherCode())) {
            String vCode = request.getVoucherCode().trim();
            appliedVoucher = voucherRepository.findByVoucherCodeIgnoreCase(vCode)
                    .orElseThrow(() -> new AppException(ErrorCode.ERR_VOUCHER_NOT_FOUND, "Mã giảm giá '" + vCode + "' không tồn tại!"));

            if (!Boolean.TRUE.equals(appliedVoucher.getIsActive())) {
                throw new AppException(ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + vCode + "' đã ngừng hoạt động!");
            }
            if (appliedVoucher.getStartDate() != null && now.isBefore(appliedVoucher.getStartDate())) {
                throw new AppException(ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + vCode + "' chưa đến thời gian có hiệu lực!");
            }
            if (appliedVoucher.getEndDate() != null && now.isAfter(appliedVoucher.getEndDate())) {
                throw new AppException(ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + vCode + "' đã hết hạn sử dụng!");
            }
            int used = appliedVoucher.getUsedCount() != null ? appliedVoucher.getUsedCount() : 0;
            if (appliedVoucher.getUsageLimit() != null && used >= appliedVoucher.getUsageLimit()) {
                throw new AppException(ErrorCode.ERR_VOUCHER_LIMIT_REACHED, "Mã giảm giá '" + vCode + "' đã hết lượt sử dụng!");
            }
            if (appliedVoucher.getMinOrderValue() != null && subtotal.compareTo(appliedVoucher.getMinOrderValue()) < 0) {
                throw new AppException(ErrorCode.ERR_VOUCHER_MIN_ORDER,
                        String.format("Đơn hàng chưa đạt giá trị tối thiểu %sđ để áp dụng mã %s!",
                                CURRENCY_FORMATTER.format(appliedVoucher.getMinOrderValue()), appliedVoucher.getCode()));
            }

            String discType = appliedVoucher.getDiscountType() != null ? appliedVoucher.getDiscountType().trim().toUpperCase() : "FIXED";
            if ("PERCENT".equals(discType) || "PERCENTAGE".equals(discType)) {
                BigDecimal pct = appliedVoucher.getDiscountValue() != null ? appliedVoucher.getDiscountValue() : BigDecimal.ZERO;
                voucherDiscount = subtotal.multiply(pct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (appliedVoucher.getMaxDiscount() != null && appliedVoucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    voucherDiscount = voucherDiscount.min(appliedVoucher.getMaxDiscount());
                }
                voucherDiscount = voucherDiscount.min(subtotal);
            } else {
                BigDecimal fixedVal = appliedVoucher.getDiscountValue() != null ? appliedVoucher.getDiscountValue() : BigDecimal.ZERO;
                voucherDiscount = fixedVal.min(subtotal);
            }

            // Tăng số lượt dùng voucher nguyên tử (chống race condition vượt usage_limit)
            int voucherUpdated = voucherRepository.incrementUsedCountAtomic(appliedVoucher.getVoucherId());
            if (voucherUpdated == 0) {
                throw new AppException(ErrorCode.ERR_VOUCHER_LIMIT_REACHED, "Mã giảm giá '" + vCode + "' vừa hết lượt sử dụng!");
            }
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(voucherDiscount).add(shippingFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // 4. Sinh mã đơn hàng an toàn chống trùng lặp (#DH-YYYYMMDD-XXXXXX - Phiếu MB01)
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long millis = System.currentTimeMillis() % 100000;
        int rand = (int) (Math.random() * 9000) + 1000;
        String orderCode = String.format("DH-%s-%05d%04d", dateStr, millis, rand);
        int attempts = 0;
        while (orderRepository.existsByOrderCode(orderCode) && attempts < 5) {
            orderCode = String.format("DH-%s-%05d%04d", dateStr, (System.currentTimeMillis() + attempts) % 100000, (int) (Math.random() * 9000) + 1000);
            attempts++;
        }

        String payMethod = StringUtils.hasText(request.getPaymentMethod()) ? request.getPaymentMethod().trim().toUpperCase() : "COD";
        String payStatus = "UNPAID";
        boolean isTransfer = "BANK_TRANSFER".equals(payMethod) || "SIMULATED_TRANSFER".equals(payMethod);
        String initialStatus = isTransfer ? "PENDING_PAYMENT" : "WAITING_CONFIRMATION";

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(currentUser)
                .voucher(appliedVoucher)
                .status(initialStatus)
                .paymentMethod(payMethod)
                .paymentStatus(payStatus)
                .recipientName(request.getRecipientName().trim())
                .recipientPhone(request.getRecipientPhone().trim())
                .shippingAddress(request.getShippingAddress().trim())
                .totalAmount(subtotal)
                .discountAmount(voucherDiscount)
                .shippingFee(shippingFee)
                .finalAmount(totalAmount)
                .note(request.getNote())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Khởi tạo bản ghi thanh toán chuyển khoản ban đầu để theo dõi giao dịch
        if (isTransfer) {
            String txnRef = "TXN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
            paymentRepository.save(Payment.builder()
                    .order(savedOrder)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER.name())
                    .paymentProvider("SIMULATED_QR")
                    .amount(totalAmount)
                    .transactionRef(txnRef)
                    .status(PaymentStatus.PENDING.name())
                    .build());
        }

        // 5. Lưu Order Items & TỰ ĐỘNG GIỮ CHỖ KHO NGUYÊN TỬ (Chống bán khống / overselling)
        for (OrderItem oi : orderItemsToSave) {
            oi.setOrder(savedOrder);
            orderItemRepository.save(oi);

            ProductVariant v = oi.getVariant();
            int reserved = inventoryRepository.reserveStockAtomic(v.getVariantId(), oi.getQuantity(), now);
            if (reserved == 0) {
                Product prod = v.getProduct();
                String prodName = prod != null ? prod.getProductName() : v.getVariantName();
                throw new AppException(ErrorCode.ERR_STOCK_INSUFFICIENT,
                        String.format("Sản phẩm %s (SKU: %s) vừa hết hàng khả dụng trong kho!", prodName, v.getSku()));
            }
        }

        if (StringUtils.hasText(idempotencyKey)) {
            String redisKey = "idempotency:order:" + currentUser.getUserId() + ":" + idempotencyKey.trim();
            tokenRedisService.setValue(redisKey, savedOrder.getOrderCode(), Duration.ofHours(24));
        }

        // 6. Ghi Order Status History
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(savedOrder)
                .changedBy(currentUser)
                .previousStatus(null)
                .newStatus(initialStatus)
                .note("Khởi tạo đơn hàng " + payMethod + " thành công (Phiếu MB01). Tồn kho đã được giữ chỗ.")
                .build());

        // 7. Xóa món đã mua khỏi giỏ hàng
        try {
            cartRepository.findByUserId(currentUser.getUserId()).ifPresent(cart -> {
                for (ProductVariant v : itemMap.keySet()) {
                    cartItemRepository.findByCartIdAndVariantId(cart.getCartId(), v.getVariantId())
                            .ifPresent(cartItemRepository::delete);
                }
            });
        } catch (Exception ex) {
            log.warn("Không thể dọn giỏ hàng sau khi đặt hàng: {}", ex.getMessage());
        }

        return CreateOrderResponse.builder()
                .orderId(savedOrder.getOrderId())
                .orderCode(savedOrder.getOrderCode())
                .status(savedOrder.getStatus())
                .subtotal(savedOrder.getSubtotal())
                .voucherDiscount(savedOrder.getVoucherDiscount())
                .shippingFee(savedOrder.getShippingFee() != null ? savedOrder.getShippingFee() : BigDecimal.ZERO)
                .totalAmount(savedOrder.getFinalTotal())
                .paymentMethod(savedOrder.getPaymentMethod())
                .paymentStatus(savedOrder.getPaymentStatus())
                .itemsCount(orderItemsToSave.size())
                .createdAt(DateTimeUtil.formatIsoInstant(savedOrder.getCreatedAt() != null ? savedOrder.getCreatedAt() : now))
                .build();
    }

    /**
     * API 8.2: [GET] /api/v1/orders/my-orders — Khách hàng tra cứu lịch sử mua hàng (Authenticated)
     */
    @Transactional(readOnly = true)
    public PageResponse<MyOrderItemResponse> getMyOrders(int page, int size, String status) {
        User currentUser = securityHelper.getCurrentUser();

        int safePage = Math.max(page, 1) - 1;
        int safeSize = (size <= 0 || size > 100) ? 5 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Order> spec = (root, query, cb) -> {
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
        Map<Long, List<OrderItemResponse>> itemsByOrderId = orderIds.isEmpty() ? Map.of() :
                orderItemRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(
                                oi -> oi.getOrder().getOrderId(),
                                Collectors.mapping(this::mapOrderItemResponse, Collectors.toList())
                        ));
        Map<Long, List<OrderStatusHistoryResponse>> historiesByOrderId = orderIds.isEmpty() ? Map.of() :
                orderStatusHistoryRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds).stream()
                        .collect(Collectors.groupingBy(
                                h -> h.getOrder().getOrderId(),
                                Collectors.mapping(this::mapOrderStatusHistoryResponse, Collectors.toList())
                        ));
        Map<Long, Shipment> myShipmentMap = orderIds.isEmpty() ? Map.of() :
                shipmentRepository.findByOrder_OrderIdIn(orderIds).stream()
                        .collect(Collectors.toMap(s -> s.getOrder().getOrderId(), s -> s, (s1, s2) -> s1));
        Map<Long, ReturnRequest> myReturnMap = orderIds.isEmpty() ? Map.of() :
                returnRequestRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds).stream()
                        .collect(Collectors.toMap(r -> r.getOrder().getOrderId(), r -> r, (r1, r2) -> r1));

        List<MyOrderItemResponse> items = orders.stream().map(order -> {
            Shipment shipment = myShipmentMap.get(order.getOrderId());
            ReturnRequest latestReturn = myReturnMap.get(order.getOrderId());
            return MyOrderItemResponse.builder()
                    .orderId(order.getOrderId())
                    .orderCode(order.getOrderCode())
                    .status(order.getStatus())
                    .recipientName(order.getRecipientName())
                    .recipientPhone(order.getRecipientPhone())
                    .shippingAddress(order.getShippingAddress())
                    .subtotal(order.getSubtotal())
                    .voucherDiscount(order.getVoucherDiscount())
                    .shippingFee(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
                    .totalAmount(order.getFinalTotal())
                    .paymentMethod(order.getPaymentMethod())
                    .paymentStatus(order.getPaymentStatus())
                    .carrier(shipment != null ? shipment.getCarrier() : null)
                    .trackingCode(shipment != null ? shipment.getTrackingCode() : null)
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
                    .items(itemsByOrderId.getOrDefault(order.getOrderId(), List.of()))
                    .statusHistory(historiesByOrderId.getOrDefault(order.getOrderId(), List.of()))
                    .build();
        }).toList();

        return PageResponse.<MyOrderItemResponse>builder()
                .content(items)
                .page(pageResult.getNumber() + 1)
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .build();
    }

    /**
     * API 8.3: [GET] /api/v1/orders/{orderCode} — Chi tiết đơn hàng theo mã #DH-XXXX (Authenticated)
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(String orderCode) {
        User currentUser = securityHelper.getCurrentUser();

        Order order = orderRepository.findByOrderCode(orderCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng có mã " + orderCode + "!"));

        // Kiểm tra quyền: STAFF/ADMIN hoặc chủ đơn hàng
        boolean isStaffOrAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.STAFF;
        if (!isStaffOrAdmin && !order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền truy cập vào thông tin đơn hàng này!");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getOrderId());
        List<OrderItemResponse> itemResponses = orderItems.stream().map(this::mapOrderItemResponse).toList();

        List<OrderStatusHistoryResponse> historyResponses = orderStatusHistoryRepository
                .findByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId()).stream()
                .map(this::mapOrderStatusHistoryResponse)
                .toList();

        Optional<Shipment> shipmentOpt = shipmentRepository.findFirstByOrder_OrderId(order.getOrderId());
        String carrier = shipmentOpt.map(Shipment::getCarrier).orElse(null);
        String trackingCode = shipmentOpt.map(Shipment::getTrackingCode).orElse(null);
        ReturnRequest latestReturn = returnRequestRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId()).orElse(null);

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
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

    /**
     * API 8.4: [POST] /api/v1/orders/{orderCode}/cancel — Khách hàng hủy đơn & Giải phóng kho (Authenticated)
     */
    @Transactional
    public CancelOrderResponse cancelOrder(String orderCode, CancelOrderRequest request) {
        User currentUser = securityHelper.getCurrentUser();

        Order order = orderRepository.findByOrderCode(orderCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng có mã " + orderCode + "!"));

        boolean isStaffOrAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.STAFF;
        if (!isStaffOrAdmin && !order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền hủy đơn hàng của người khác!");
        }

        // Ràng buộc trạng thái hủy
        String currentStatus = order.getStatus();
        if (!"WAITING_CONFIRMATION".equalsIgnoreCase(currentStatus) && !"PENDING_PAYMENT".equalsIgnoreCase(currentStatus)) {
            throw new AppException(ErrorCode.ERR_ORDER_CANNOT_CANCEL,
                    "Không thể hủy đơn hàng do đơn đã được xác nhận hoặc đang trong quá trình giao hàng!");
        }

        LocalDateTime now = LocalDateTime.now();

        // Rollback giải phóng tồn giữ chỗ (reserved_qty -= quantity)
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
        for (OrderItem item : items) {
            ProductVariant v = item.getVariant();
            if (v != null) {
                inventoryRepository.releaseStockAtomic(v.getVariantId(), item.getQuantity(), now);
            }
        }

        // Hoàn trả lượt sử dụng voucher nếu có
        if (order.getVoucher() != null) {
            voucherRepository.decrementUsedCountAtomic(order.getVoucher().getVoucherId());
        }

        // Cập nhật đơn hàng
        order.setStatus("CANCELLED");
        order.setCancelReason(request.getReason().trim());
        order.setUpdatedAt(now);
        orderRepository.save(order);

        // Ghi lịch sử trạng thái
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .changedBy(currentUser)
                .previousStatus(currentStatus)
                .newStatus("CANCELLED")
                .note("Hủy đơn hàng: " + request.getReason().trim())
                .build());

        return CancelOrderResponse.builder()
                .orderCode(order.getOrderCode())
                .status("CANCELLED")
                .cancelReason(request.getReason().trim())
                .releasedHoldStock(true)
                .cancelledAt(DateTimeUtil.formatIsoInstant(now))
                .build();
    }

    /**
     * API 8.5: [GET] /api/v1/admin/orders — Quản trị toàn bộ danh sách đơn hàng (STAFF / ADMIN)
     */
    @Transactional(readOnly = true)
    public AdminOrderListResponse getAdminOrders(int page, int size, String status, String paymentStatus) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1) - 1;
        int safeSize = (size <= 0 || size > 100) ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(status) && !status.equalsIgnoreCase("ALL")) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (StringUtils.hasText(paymentStatus) && !paymentStatus.equalsIgnoreCase("ALL")) {
                predicates.add(cb.equal(cb.upper(root.get("paymentStatus")), paymentStatus.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> pageResult = orderRepository.findAll(spec, pageable);
        List<Order> orders = pageResult.getContent();
        List<Long> orderIds = orders.stream().map(Order::getOrderId).toList();

        Map<Long, List<OrderItem>> orderItemsMap = orderIds.isEmpty() ? Map.of() :
                orderItemRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(oi -> oi.getOrder().getOrderId()));

        Map<Long, Shipment> shipmentMap = orderIds.isEmpty() ? Map.of() :
                shipmentRepository.findByOrder_OrderIdIn(orderIds).stream()
                        .collect(Collectors.toMap(s -> s.getOrder().getOrderId(), s -> s, (s1, s2) -> s1));
        Map<Long, List<OrderStatusHistoryResponse>> historiesMap = orderIds.isEmpty() ? Map.of() :
                orderStatusHistoryRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds).stream()
                        .collect(Collectors.groupingBy(
                                h -> h.getOrder().getOrderId(),
                                Collectors.mapping(this::mapOrderStatusHistoryResponse, Collectors.toList())
                        ));

        Map<Long, ReturnRequest> adminReturnMap = orderIds.isEmpty() ? Map.of() :
                returnRequestRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds).stream()
                        .collect(Collectors.toMap(r -> r.getOrder().getOrderId(), r -> r, (r1, r2) -> r1));

        List<AdminOrderItemResponse> items = orders.stream().map(order -> {
            List<OrderItem> orderItems = orderItemsMap.getOrDefault(order.getOrderId(), List.of());
            String itemsSummary = buildItemsSummary(orderItems);
            Shipment shipment = shipmentMap.get(order.getOrderId());
            ReturnRequest latestReturn = adminReturnMap.get(order.getOrderId());

            return AdminOrderItemResponse.builder()
                    .orderId(order.getOrderId())
                    .orderCode(order.getOrderCode())
                    .recipientName(order.getRecipientName())
                    .recipientPhone(order.getRecipientPhone())
                    .shippingAddress(order.getShippingAddress())
                    .paymentMethod(order.getPaymentMethod())
                    .paymentStatus(order.getPaymentStatus())
                    .subtotal(order.getSubtotal())
                    .voucherDiscount(order.getVoucherDiscount())
                    .shippingFee(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
                    .totalAmount(order.getFinalTotal())
                    .status(order.getStatus())
                    .carrier(shipment != null ? shipment.getCarrier() : null)
                    .trackingCode(shipment != null ? shipment.getTrackingCode() : null)
                    .cancelReason(order.getCancelReason())
                    .returnId(latestReturn != null ? latestReturn.getReturnId() : null)
                    .returnStatus(latestReturn != null ? latestReturn.getStatus() : null)
                    .returnReason(latestReturn != null ? latestReturn.getReason() : null)
                    .returnRejectReason(latestReturn != null ? latestReturn.getRejectReason() : null)
                    .returnInspectionResult(latestReturn != null ? latestReturn.getInspectionResult() : null)
                    .returnRefundAmount(latestReturn != null ? latestReturn.getRefundAmount() : null)
                    .itemsSummary(itemsSummary)
                    .createdAt(DateTimeUtil.formatIsoInstant(order.getCreatedAt()))
                    .updatedAt(DateTimeUtil.formatIsoInstant(order.getUpdatedAt()))
                    .completedAt(DateTimeUtil.formatIsoInstant(order.getCompletedAt()))
                    .items(orderItems.stream().map(this::mapOrderItemResponse).toList())
                    .statusHistory(historiesMap.getOrDefault(order.getOrderId(), List.of()))
                    .build();
        }).toList();

        Map<String, Long> statusCounts = orderRepository.countGroupByStatus().stream()
                .filter(r -> r != null && r[0] != null && r[1] != null)
                .collect(Collectors.toMap(r -> (String) r[0], r -> ((Number) r[1]).longValue(), (a, b) -> a));

        PipelineSummaryResponse pipeline = PipelineSummaryResponse.builder()
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

    /**
     * API 8.6: [POST] /api/v1/admin/orders/{id}/confirm — Xác nhận đơn MB02 & Tự động tạo Vận đơn (STAFF / ADMIN)
     */
    @Transactional
    public ConfirmOrderResponse confirmOrder(Long id, ConfirmOrderRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + id + "!"));

        String currentStatus = order.getStatus();
        if (!"WAITING_CONFIRMATION".equalsIgnoreCase(currentStatus) && !"PENDING_PAYMENT".equalsIgnoreCase(currentStatus)) {
            throw new AppException(ErrorCode.ERR_ORDER_INVALID_STATE,
                    "Chỉ có thể xác nhận đơn hàng đang ở trạng thái Chờ xác nhận hoặc Chờ thanh toán!");
        }

        LocalDateTime now = LocalDateTime.now();

        order.setStatus("CONFIRMED");
        order.setUpdatedAt(now);
        orderRepository.save(order);

        // Sinh mã tracking vận đơn: GHN-YYYYMMDD-OrderId
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String trackingCode = "GHN-" + dateStr + order.getOrderId();

        BigDecimal codAmt = "COD".equalsIgnoreCase(order.getPaymentMethod()) ? order.getFinalTotal() : BigDecimal.ZERO;

        Shipment shipment = Shipment.builder()
                .order(order)
                .carrierName(request.getCarrier().trim())
                .trackingNumber(trackingCode)
                .shippingStatus("PREPARING")
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .codAmount(codAmt)
                .notes(request.getNote())
                .updatedBy(operator)
                .build();
        Shipment savedShipment = shipmentRepository.save(shipment);

        // Ghi lịch sử
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .changedBy(operator)
                .previousStatus(currentStatus)
                .newStatus("CONFIRMED")
                .note(StringUtils.hasText(request.getNote()) ? request.getNote() : "Xác nhận đơn hàng MB02 và tạo vận đơn")
                .build());

        // Ghi Audit Log
        auditLogRepository.log(operator, "CONFIRM_ORDER", "Orders", order.getOrderId(),
                "status: " + currentStatus, "status: CONFIRMED, tracking: " + trackingCode);

        String confirmedByName = operator.getFullName() != null
                ? operator.getFullName() + " (" + (operator.getRole() != null ? operator.getRole().name() : "Nhân viên") + ")"
                : "Nhân viên";

        ShipmentDetailResponse shipmentDetail = ShipmentDetailResponse.builder()
                .shipmentId(savedShipment.getShipmentId())
                .trackingCode(savedShipment.getTrackingCode())
                .carrier(savedShipment.getCarrier())
                .status(savedShipment.getStatus())
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

    /**
     * API 8.7: [POST] /api/v1/admin/orders/{id}/pack-export — Đóng gói & Xuất kho vật lý MB03 (STAFF / ADMIN)
     */
    @Transactional
    public PackExportOrderResponse packAndExportOrder(Long id, PackExportOrderRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + id + "!"));

        String currentStatus = order.getStatus();
        if (!"CONFIRMED".equalsIgnoreCase(currentStatus) && !"PREPARING".equalsIgnoreCase(currentStatus)) {
            throw new AppException(ErrorCode.ERR_ORDER_NOT_CONFIRMED,
                    "Đơn hàng phải được duyệt xác nhận (CONFIRMED) trước khi thực hiện xuất kho MB03!");
        }

        LocalDateTime now = LocalDateTime.now();
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
        List<ExportedItemResponse> exportedItems = new ArrayList<>();

        for (OrderItem item : items) {
            ProductVariant v = item.getVariant();
            if (v == null) continue;

            Inventory inv = inventoryRepository.findByVariant_VariantId(v.getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.ERR_STOCK_INSUFFICIENT,
                            "Không tìm thấy tồn kho cho SKU " + v.getSku() + "!"));

            int onHandBefore = inv.getOnHandQty();
            int updatedRows = inventoryRepository.deductStockAndReservedAtomic(v.getVariantId(), item.getQuantity(), now);
            if (updatedRows == 0) {
                throw new AppException(ErrorCode.ERR_STOCK_INSUFFICIENT,
                        "Tồn kho vật lý của SKU " + v.getSku() + " không đủ để xuất kho (cần " + item.getQuantity() + ", hiện có " + onHandBefore + ")!");
            }
            int onHandAfter = onHandBefore - item.getQuantity();

            // Ghi sổ cái bất biến Inventory_Transactions (MB03 EXPORT)
            inventoryTransactionRepository.save(InventoryTransaction.builder()
                    .variant(v)
                    .order(order)
                    .performedBy(operator)
                    .transactionType("EXPORT")
                    .formCode("MB03")
                    .quantity(-item.getQuantity())
                    .qtyBefore(onHandBefore)
                    .qtyAfter(onHandAfter)
                    .reason("Xuất kho giao đơn #" + order.getOrderCode() + (StringUtils.hasText(request.getExportNote()) ? ": " + request.getExportNote() : ""))
                    .build());

            exportedItems.add(ExportedItemResponse.builder()
                    .sku(v.getSku())
                    .quantity(item.getQuantity())
                    .remainingOnHand(onHandAfter)
                    .build());
        }

        // Cập nhật trạng thái đơn sang SHIPPING
        order.setStatus("SHIPPING");
        order.setUpdatedAt(now);
        orderRepository.save(order);

        // Cập nhật trạng thái vận đơn sang IN_TRANSIT
        Shipment shipment = shipmentRepository.findFirstByOrder_OrderId(order.getOrderId())
                .orElseGet(() -> Shipment.builder().order(order).carrierName("Giao Hàng Nhanh Express").trackingNumber("GHN-" + order.getOrderId()).build());

        shipment.setShippingStatus("IN_TRANSIT");
        shipment.setShippedAt(now);
        shipment.setUpdatedBy(operator);
        if (StringUtils.hasText(request.getExportNote())) {
            shipment.setNotes(request.getExportNote());
        }
        Shipment savedShipment = shipmentRepository.save(shipment);

        // Ghi lịch sử
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .changedBy(operator)
                .previousStatus(currentStatus)
                .newStatus("SHIPPING")
                .note("Đóng gói & Xuất kho MB03, bàn giao đơn vị vận chuyển" + (StringUtils.hasText(request.getAssignedShipper()) ? " (Tài xế: " + request.getAssignedShipper() + ")" : ""))
                .build());

        // Ghi Audit Log
        auditLogRepository.log(operator, "PACK_EXPORT_ORDER", "Orders", order.getOrderId(),
                "status: " + currentStatus, "status: SHIPPING, form: MB03");

        String exportedByName = operator.getFullName() != null
                ? operator.getFullName() + " (" + (operator.getRole() != null ? operator.getRole().name() : "Thủ kho") + ")"
                : "Thủ kho";

        ShipmentDetailResponse shipmentDetail = ShipmentDetailResponse.builder()
                .shipmentId(savedShipment.getShipmentId())
                .trackingCode(savedShipment.getTrackingCode())
                .carrier(savedShipment.getCarrier())
                .status(savedShipment.getStatus())
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

    private OrderStatusHistoryResponse mapOrderStatusHistoryResponse(OrderStatusHistory h) {
        String changedByName = "Hệ thống";
        if (h.getChangedBy() != null) {
            changedByName = h.getChangedBy().getFullName() != null
                    ? h.getChangedBy().getFullName() + " (" + (h.getChangedBy().getRole() != null ? h.getChangedBy().getRole().name() : "Người dùng") + ")"
                    : "Người dùng";
        }
        return OrderStatusHistoryResponse.builder()
                .historyId(h.getHistoryId())
                .status(h.getToStatus())
                .changedBy(changedByName)
                .note(h.getReason())
                .changedAt(DateTimeUtil.formatIsoInstant(h.getCreatedAt()))
                .build();
    }

    private OrderItemResponse mapOrderItemResponse(OrderItem oi) {
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
    }

    private String buildItemsSummary(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "Không có sản phẩm";
        }
        OrderItem first = items.get(0);
        String firstName = first.getProductNameSnapshot() != null ? first.getProductNameSnapshot() : "Sản phẩm";
        int firstQty = first.getQuantity() != null ? first.getQuantity() : 1;

        if (items.size() == 1) {
            return String.format("%s (x%d)", firstName, firstQty);
        } else {
            return String.format("%s (x%d) + %d sản phẩm khác", firstName, firstQty, items.size() - 1);
        }
    }

    /**
     * API Xác nhận đã nhận tiền chuyển khoản đơn hàng (STAFF / ADMIN)
     */
    @Transactional
    public OrderDetailResponse confirmOrderPayment(Long orderId, String note) {
        User operator = securityHelper.getCurrentUser();
        if (operator.getRole() != Role.ADMIN && operator.getRole() != Role.STAFF) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ quản trị viên hoặc nhân viên mới có quyền xác nhận thanh toán!");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + orderId + "!"));

        order.setPaymentStatus("PAID");
        String prevStatus = order.getStatus();
        if ("PENDING_PAYMENT".equalsIgnoreCase(prevStatus)) {
            order.setStatus("WAITING_CONFIRMATION");
        }
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);

        // Cập nhật hoặc tạo mới bản ghi Payment
        Payment payment = paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId)
                .orElseGet(() -> Payment.builder()
                        .order(updated)
                        .paymentMethod(updated.getPaymentMethod() != null ? updated.getPaymentMethod() : "BANK_TRANSFER")
                        .paymentProvider("SIMULATED_QR")
                        .amount(updated.getFinalTotal())
                        .transactionRef("TXN-" + System.currentTimeMillis())
                        .build());
        payment.setStatus(PaymentStatus.SUCCESS.name());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        String noteText = StringUtils.hasText(note) ? note.trim() : "Nhân viên xác nhận đã nhận tiền chuyển khoản ngân hàng";
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(updated)
                .changedBy(operator)
                .previousStatus(prevStatus)
                .newStatus(updated.getStatus())
                .note(noteText)
                .build());

        try {
            auditLogRepository.log(operator, "CONFIRM_PAYMENT", "Orders", orderId, "paymentStatus: UNPAID", "paymentStatus: PAID");
        } catch (Exception e) {
            log.warn("Không thể ghi audit log confirmOrderPayment: {}", e.getMessage());
        }

        return getOrderDetail(updated.getOrderCode());
    }

    /**
     * API Khách hàng thông báo đã chuyển khoản thành công (Authenticated)
     */
    @Transactional
    public OrderDetailResponse customerNotifyPayment(Long orderId, String note) {
        User currentUser = securityHelper.getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + orderId + "!"));

        boolean isStaffOrAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.STAFF;
        if (!isStaffOrAdmin && (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId()))) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thao tác trên đơn hàng này!");
        }

        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            order.setPaymentStatus("PENDING"); // Đang chờ kế toán/nhân viên đối soát sao kê
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            String noteText = StringUtils.hasText(note) ? note.trim() : "Khách hàng đã xác nhận chuyển khoản thành công, chờ kiểm tra giao dịch";
            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .changedBy(currentUser)
                    .previousStatus(order.getStatus())
                    .newStatus(order.getStatus())
                    .note(noteText)
                    .build());
        }

        return getOrderDetail(order.getOrderCode());
    }

    /**
     * Tự động quét và hủy các đơn hàng chờ thanh toán QR đã hết hạn, giải phóng tồn kho
     */
    @Transactional
    public int cancelExpiredOrders() {
        int expireMins = paymentExpireMinutes > 0 ? paymentExpireMinutes : 30;
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(expireMins);
        List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(expiredBefore);
        if (expiredOrders.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        int count = 0;

        for (Order order : expiredOrders) {
            // 1. Rollback giải phóng tồn giữ chỗ (reserved_qty -= quantity)
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getOrderId());
            for (OrderItem item : items) {
                ProductVariant v = item.getVariant();
                if (v != null) {
                    inventoryRepository.releaseStockAtomic(v.getVariantId(), item.getQuantity(), now);
                }
            }

            // 2. Hoàn trả voucher nếu có
            if (order.getVoucher() != null) {
                voucherRepository.decrementUsedCountAtomic(order.getVoucher().getVoucherId());
            }

            // 3. Cập nhật trạng thái CANCELLED
            order.setStatus("CANCELLED");
            order.setCancelReason("Mã QR hết hiệu lực thanh toán (Quá thời hạn giữ chỗ " + expireMins + " phút) - Hệ thống tự động hủy đơn và giải phóng tồn kho");
            order.setUpdatedAt(now);
            orderRepository.save(order);

            // 4. Ghi lịch sử OrderStatusHistory
            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .changedBy(null)
                    .previousStatus("PENDING_PAYMENT")
                    .newStatus("CANCELLED")
                    .note("Hệ thống tự động hủy đơn do quá hạn thanh toán QR " + expireMins + " phút và đã nhả toàn bộ tồn kho giữ chỗ.")
                    .build());

            count++;
        }
        return count;
    }
}
