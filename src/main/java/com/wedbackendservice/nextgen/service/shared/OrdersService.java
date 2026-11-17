package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.util.DateTimeUtil;

import com.wedbackendservice.nextgen.dto.portal.request.order.*;
import com.wedbackendservice.nextgen.dto.client.request.order.*;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
import com.wedbackendservice.nextgen.dto.portal.response.order.*;
import com.wedbackendservice.nextgen.dto.client.response.order.*;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.OrderStatus;
import com.wedbackendservice.nextgen.enums.InventoryTransactionType;
import com.wedbackendservice.nextgen.enums.PaymentMethod;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.enums.ShipmentStatus;
import com.wedbackendservice.nextgen.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.mapper.OrderMapper;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Clock;
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
    private final NotificationRepository notificationRepository;
    private final OrderMapper orderMapper;
    private final OutboxService outboxService;
    private final InventoryService inventoryService;
    private final ShippingFeeCalculator shippingFeeCalculator;
    private final OrderExpirationService orderExpirationService;
    private final OrderPostCommitService orderPostCommitService;
    private final OrderStateService orderStateService;
    private final Clock clock;
    @Value("${payment.bank-transfer.expire-minutes:30}")
    private int paymentExpireMinutes = 30;
    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("#,###");

    /**
     * API 8.1: [POST] /api/v1/orders — Đặt hàng Checkout (Khởi tạo MB01 & Giữ chỗ tồn kho đồng bộ) (Authenticated)
     */
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        User currentUser = securityHelper.getCurrentUser();
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. Kiểm tra Idempotency key bằng SETNX chống race condition
        String redisKey = null;
        if (StringUtils.hasText(idempotencyKey)) {
            redisKey = "idempotency:order:" + currentUser.getUserId() + ":" + idempotencyKey.trim();
            boolean acquired = tokenRedisService.setIfAbsent(redisKey, "PROCESSING", Duration.ofMinutes(2));
            if (!acquired) {
                String existingCode = tokenRedisService.getValue(redisKey);
                if ("PROCESSING".equals(existingCode)) {
                    throw new AppException(ErrorCode.ERR_ORDER_PROCESSING, "Yêu cầu tạo đơn hàng đang được xử lý, vui lòng chờ trong giây lát!");
                }
                if (StringUtils.hasText(existingCode)) {
                    Optional<Order> existing = orderRepository.findByOrderCode(existingCode);
                    if (existing.isPresent()) {
                        List<OrderItem> existingItems = orderItemRepository.findByOrderId(existing.get().getOrderId());
                        return toCreateOrderResponse(existing.get(), existingItems != null ? existingItems.size() : 0);
                    }
                }
                throw new AppException(ErrorCode.ERR_ORDER_PROCESSING, "Yêu cầu tạo đơn hàng đang được xử lý hoặc đã hoàn tất!");
            }
        }
        final String finalRedisKey = redisKey;

        try {
            // 2. Validate và gộp sản phẩm trùng lặp
            Map<Long, Integer> consolidatedQty = validateAndConsolidateItems(request.getItems());

            // 3. Batch load ProductVariants (Tránh N+1)
            List<Long> variantIds = new ArrayList<>(consolidatedQty.keySet());
            List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);
            if (variants == null || variants.size() != variantIds.size()) {
                throw new AppException(ErrorCode.ERR_VARIANT_NOT_FOUND, "Có biến thể sản phẩm không tồn tại trong hệ thống!");
            }

            Map<Long, ProductVariant> variantMap = new HashMap<>();
            for (ProductVariant v : variants) {
                variantMap.put(v.getVariantId(), v);
                if (v.getStatus() != null && !"ACTIVE".equalsIgnoreCase(v.getStatus())) {
                    String prodName = v.getProduct() != null ? v.getProduct().getProductName() : v.getVariantName();
                    throw new AppException(ErrorCode.PRODUCT_INACTIVE,
                            String.format("Biến thể '%s' (SKU: %s) hiện đã ngừng kinhdong!", prodName, v.getSku()));
                }
                if (v.getProduct() != null && v.getProduct().getStatus() != null && !"ACTIVE".equalsIgnoreCase(v.getProduct().getStatus())) {
                    throw new AppException(ErrorCode.PRODUCT_INACTIVE,
                            String.format("Sản phẩm '%s' hiện đã ngừng kinh doanh!", v.getProduct().getProductName()));
                }
            }

            // 4. Batch load Promotions (Lấy khuyến mãi thấp nhất, tránh N+1)
            List<PromotionVariant> activePromos = promotionVariantRepository.findActivePromotionsForVariantIds(variantIds, now);
            Map<Long, BigDecimal> bestPromoPriceMap = new HashMap<>();
            if (activePromos != null) {
                for (PromotionVariant pv : activePromos) {
                    if (pv != null && pv.getVariant() != null && pv.getVariant().getVariantId() != null && pv.getPromotionalPrice() != null) {
                        bestPromoPriceMap.merge(pv.getVariant().getVariantId(), pv.getPromotionalPrice(), BigDecimal::min);
                    }
                }
            }

            // 5. Tính tiền sản phẩm
            BigDecimal subtotal = BigDecimal.ZERO;
            List<OrderItem> orderItemsToSave = new ArrayList<>();
            List<OrderCreatedEvent.OrderItemDto> eventItems = new ArrayList<>();

            for (Map.Entry<Long, Integer> entry : consolidatedQty.entrySet()) {
                Long vId = entry.getKey();
                int qty = entry.getValue();
                ProductVariant variant = variantMap.get(vId);

                BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
                BigDecimal promoPrice = bestPromoPriceMap.get(vId);
                if (promoPrice != null && promoPrice.compareTo(unitPrice) < 0) {
                    unitPrice = promoPrice;
                }

                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                subtotal = subtotal.add(lineTotal);

                Product prod = variant.getProduct();
                String prodName = prod != null
                        ? prod.getProductName() + (variant.getVariantName() != null && !variant.getVariantName().isBlank() ? " (" + variant.getVariantName() + ")" : "")
                        : variant.getVariantName();
                String rom = variant.getRomGb() != null ? variant.getRomGb() + "GB" : null;

                orderItemsToSave.add(OrderItem.builder()
                        .variant(variant)
                        .productNameSnapshot(prodName)
                        .skuSnapshot(variant.getSku())
                        .colorSnapshot(variant.getColor())
                        .romSnapshot(rom)
                        .imageUrl(variant.getImageUrl())
                        .price(unitPrice)
                        .quantity(qty)
                        .totalPrice(lineTotal)
                        .build());

                eventItems.add(OrderCreatedEvent.OrderItemDto.builder()
                        .variantId(vId)
                        .quantity(qty)
                        .unitPrice(unitPrice)
                        .build());
            }

            // 6. Áp dụng Voucher (nếu có)
            BigDecimal voucherDiscount = BigDecimal.ZERO;
            Voucher appliedVoucher = null;
            if (StringUtils.hasText(request.getVoucherCode())) {
                appliedVoucher = applyVoucherAtomic(request.getVoucherCode().trim(), subtotal, now);
                voucherDiscount = calculateVoucherDiscount(appliedVoucher, subtotal);
            }

            // 7. Tính phí ship độc lập từ Server (Không tin client shippingFee)
            BigDecimal shippingFee = shippingFeeCalculator.calculateFee(request.getShippingMethod());
            BigDecimal totalAmount = subtotal.subtract(voucherDiscount).add(shippingFee).max(BigDecimal.ZERO);

            // So khớp chính xác expectedTotalAmount nếu client gửi lên
            if (request.getExpectedTotalAmount() != null && totalAmount.compareTo(request.getExpectedTotalAmount()) != 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        String.format("Giá sản phẩm hoặc khuyến mãi vừa thay đổi (Tổng tiền mới: %sđ). Vui lòng xác nhận lại đơn hàng!",
                                CURRENCY_FORMATTER.format(totalAmount)));
            }

            // 8. Validate Payment Method Whitelist (Chỉ COD hoặc BANK_TRANSFER)
            String payMethod = validatePaymentMethod(request.getPaymentMethod());
            boolean isTransfer = PaymentMethod.BANK_TRANSFER.name().equals(payMethod);
            String initialStatus = isTransfer ? OrderStatus.PENDING_PAYMENT.name() : OrderStatus.WAITING_CONFIRMATION.name();

            // 9. Lưu Order với mã đơn hàng duy nhất không đụng độ
            Order savedOrder = saveOrder(currentUser, appliedVoucher, initialStatus, payMethod,
                    request, subtotal, voucherDiscount, shippingFee, totalAmount);

            // 10. Lưu Order Items
            for (OrderItem oi : orderItemsToSave) {
                oi.setOrder(savedOrder);
                orderItemRepository.save(oi);
            }

            // 11. Tạo bản ghi Payment PENDING nếu là BANK_TRANSFER
            if (isTransfer) {
                Payment initialPayment = Payment.builder()
                        .order(savedOrder)
                        .paymentMethod(PaymentMethod.BANK_TRANSFER.name())
                        .paymentProvider("VIETQR_DEMO")
                        .amount(totalAmount)
                        .status(PaymentStatus.PENDING.name())
                        .build();
                paymentRepository.save(initialPayment);
            }

            // 12. GIỮ KHO ĐỒNG BỘ NGUYÊN TỬ (Chốt phương án A.1)
            // COD không hết hạn giữ chỗ thanh toán (holdMinutes = 0); BANK_TRANSFER giữ theo paymentExpireMinutes
            int holdMinutes = isTransfer ? paymentExpireMinutes : 0;
            boolean stockReserved = inventoryService.reserveStockForOrder(savedOrder.getOrderId(), eventItems, holdMinutes);
            if (!stockReserved) {
                throw new AppException(ErrorCode.ERR_STOCK_INSUFFICIENT,
                        "Một số sản phẩm trong đơn hàng vừa hết hàng hoặc không đủ số lượng khả dụng!");
            }

            // 13. Ghi Outbox Event: OrderCreated
            OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
                    .orderId(savedOrder.getOrderId())
                    .orderCode(savedOrder.getOrderCode())
                    .userId(currentUser.getUserId())
                    .recipientName(savedOrder.getRecipientName())
                    .recipientPhone(savedOrder.getRecipientPhone())
                    .shippingAddress(savedOrder.getShippingAddress())
                    .paymentMethod(payMethod)
                    .totalAmount(totalAmount)
                    .items(eventItems)
                    .build();

            outboxService.saveEvent("ORDER", savedOrder.getOrderCode(), "OrderCreated",
                    KafkaTopics.ORDER_CREATED, orderCreatedEvent);

            // 14. Ghi Order Status History
            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(savedOrder)
                    .changedBy(currentUser)
                    .previousStatus(null)
                    .newStatus(initialStatus)
                    .note("Khởi tạo đơn hàng " + initialStatus + " thành công. Đã khóa giữ chỗ tồn kho an toàn.")
                    .build());

            // 15. Transaction Synchronization: Cập nhật Redis & Dọn giỏ hàng & Gửi thông báo AFTER_COMMIT
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            if (finalRedisKey != null) {
                                tokenRedisService.setValue(finalRedisKey, savedOrder.getOrderCode(), Duration.ofHours(24));
                            }
                        } catch (Exception ex) {
                            log.warn("[OrdersService] Lưu Redis idempotency key sau commit thất bại: {}", ex.getMessage());
                        }

                        try {
                            orderPostCommitService.cleanupCartAndSendNotification(
                                    currentUser.getUserId(), consolidatedQty.keySet(),
                                    savedOrder.getOrderId(), savedOrder.getOrderCode(),
                                    isTransfer, paymentExpireMinutes);
                        } catch (Exception ex) {
                            log.warn("[OrdersService] Post-commit dọn cart & notification thất bại: {}", ex.getMessage());
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        try {
                            if (status != TransactionSynchronization.STATUS_COMMITTED && finalRedisKey != null) {
                                tokenRedisService.deleteKey(finalRedisKey);
                            }
                        } catch (Exception ex) {
                            log.warn("[OrdersService] Xóa Redis key sau rollback thất bại: {}", ex.getMessage());
                        }
                    }
                });
            } else {
                try {
                    if (finalRedisKey != null) {
                        tokenRedisService.setValue(finalRedisKey, savedOrder.getOrderCode(), Duration.ofHours(24));
                    }
                } catch (Exception ex) {
                    log.warn("[OrdersService] Lưu Redis idempotency key thất bại: {}", ex.getMessage());
                }

                try {
                    orderPostCommitService.cleanupCartAndSendNotification(
                            currentUser.getUserId(), consolidatedQty.keySet(),
                            savedOrder.getOrderId(), savedOrder.getOrderCode(),
                            isTransfer, paymentExpireMinutes);
                } catch (Exception ex) {
                    log.warn("[OrdersService] Post-commit dọn cart & notification thất bại: {}", ex.getMessage());
                }
            }

            return toCreateOrderResponse(savedOrder, orderItemsToSave.size());
        } catch (Exception ex) {
            if (finalRedisKey != null) {
                try {
                    tokenRedisService.deleteKey(finalRedisKey);
                } catch (Exception ignored) {}
            }
            throw ex;
        }
    }

    private Map<Long, Integer> validateAndConsolidateItems(List<OrderItemRequest> items) {
        Map<Long, Integer> consolidated = new LinkedHashMap<>();
        if (items != null) {
            for (OrderItemRequest req : items) {
                if (req.getVariantId() != null && req.getQuantity() != null && req.getQuantity() > 0) {
                    consolidated.merge(req.getVariantId(), req.getQuantity(), Integer::sum);
                }
            }
        }
        if (consolidated.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sách sản phẩm không hợp lệ!");
        }
        return consolidated;
    }

    private Voucher applyVoucherAtomic(String vCode, BigDecimal subtotal, LocalDateTime now) {
        Voucher appliedVoucher = voucherRepository.findByVoucherCodeIgnoreCase(vCode)
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
        if (appliedVoucher.getMinOrderValue() != null && subtotal.compareTo(appliedVoucher.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.ERR_VOUCHER_MIN_ORDER,
                    String.format("Đơn hàng chưa đạt giá trị tối thiểu %sđ để áp dụng mã %s!",
                            CURRENCY_FORMATTER.format(appliedVoucher.getMinOrderValue()), appliedVoucher.getCode()));
        }
        if (appliedVoucher.getUsageLimit() != null && appliedVoucher.getUsedCount() != null && appliedVoucher.getUsedCount() >= appliedVoucher.getUsageLimit()) {
            throw new AppException(ErrorCode.ERR_VOUCHER_LIMIT_REACHED, "Mã giảm giá '" + vCode + "' đã hết lượt sử dụng trên hệ thống!");
        }

        int voucherUpdated = voucherRepository.incrementUsedCountAtomic(appliedVoucher.getVoucherId());
        if (voucherUpdated == 0) {
            throw new AppException(ErrorCode.ERR_VOUCHER_LIMIT_REACHED, "Mã giảm giá '" + vCode + "' vừa hết lượt sử dụng!");
        }
        return appliedVoucher;
    }

    private BigDecimal calculateVoucherDiscount(Voucher appliedVoucher, BigDecimal subtotal) {
        if (appliedVoucher == null) return BigDecimal.ZERO;
        String discType = appliedVoucher.getDiscountType() != null ? appliedVoucher.getDiscountType().trim().toUpperCase() : "FIXED";
        BigDecimal voucherDiscount;
        if ("PERCENT".equals(discType) || "PERCENTAGE".equals(discType)) {
            BigDecimal pct = appliedVoucher.getDiscountValue() != null ? appliedVoucher.getDiscountValue() : BigDecimal.ZERO;
            voucherDiscount = subtotal.multiply(pct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (appliedVoucher.getMaxDiscount() != null && appliedVoucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                voucherDiscount = voucherDiscount.min(appliedVoucher.getMaxDiscount());
            }
        } else {
            BigDecimal fixedVal = appliedVoucher.getDiscountValue() != null ? appliedVoucher.getDiscountValue() : BigDecimal.ZERO;
            voucherDiscount = fixedVal.min(subtotal);
        }
        return voucherDiscount.min(subtotal);
    }

    private String validatePaymentMethod(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Phương thức thanh toán không được để trống!");
        }
        String clean = raw.trim().toUpperCase();
        if (PaymentMethod.COD.name().equals(clean)) {
            return PaymentMethod.COD.name();
        }
        if (PaymentMethod.BANK_TRANSFER.name().equals(clean) || "SIMULATED_TRANSFER".equals(clean)) {
            return PaymentMethod.BANK_TRANSFER.name();
        }
        throw new AppException(ErrorCode.INVALID_REQUEST, "Phương thức thanh toán '" + raw + "' không hợp lệ!");
    }

    private Order saveOrder(User user, Voucher voucher, String status, String payMethod,
                            CreateOrderRequest request, BigDecimal subtotal, BigDecimal discount,
                            BigDecimal shippingFee, BigDecimal total) {
        String dateStr = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long timestampSuffix = System.currentTimeMillis() % 100000000L;
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        String code = String.format("DH-%s-%08d%04d", dateStr, timestampSuffix, randomPart);

        Order order = Order.builder()
                .orderCode(code)
                .user(user)
                .voucher(voucher)
                .status(status)
                .paymentMethod(payMethod)
                .paymentStatus(PaymentStatus.UNPAID.name())
                .recipientName(request.getRecipientName() != null ? request.getRecipientName().trim() : "")
                .recipientPhone(request.getRecipientPhone() != null ? request.getRecipientPhone().trim() : "")
                .shippingAddress(request.getShippingAddress() != null ? request.getShippingAddress().trim() : "")
                .totalAmount(subtotal)
                .discountAmount(discount)
                .shippingFee(shippingFee)
                .finalAmount(total)
                .note(request.getNote())
                .build();
        return orderRepository.save(order);
    }

    private CreateOrderResponse toCreateOrderResponse(Order order, int itemsCount) {
        return CreateOrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .voucherDiscount(order.getVoucherDiscount())
                .shippingFee(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
                .totalAmount(order.getFinalTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .itemsCount(itemsCount)
                .createdAt(DateTimeUtil.formatIsoInstant(order.getCreatedAt()))
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
                                Collectors.mapping(orderMapper::toOrderItemResponse, Collectors.toList())
                        ));
        Map<Long, List<OrderStatusHistoryResponse>> historiesByOrderId = orderIds.isEmpty() ? Map.of() :
                orderStatusHistoryRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds).stream()
                        .collect(Collectors.groupingBy(
                                h -> h.getOrder().getOrderId(),
                                Collectors.mapping(orderMapper::toOrderStatusHistoryResponse, Collectors.toList())
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
                    .shippingFee(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
                    .totalAmount(order.getFinalTotal())
                    .paymentMethod(order.getPaymentMethod())
                    .paymentStatus(payStatus)
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
        List<OrderItemResponse> itemResponses = orderItems.stream().map(orderMapper::toOrderItemResponse).toList();

        List<OrderStatusHistoryResponse> historyResponses = orderStatusHistoryRepository
                .findByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId()).stream()
                .map(orderMapper::toOrderStatusHistoryResponse)
                .toList();

        Optional<Shipment> shipmentOpt = shipmentRepository.findFirstByOrder_OrderId(order.getOrderId());
        String carrier = shipmentOpt.map(Shipment::getCarrier).orElse(null);
        String trackingCode = shipmentOpt.map(Shipment::getTrackingCode).orElse(null);
        ReturnRequest latestReturn = returnRequestRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId()).orElse(null);

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

        LocalDateTime now = LocalDateTime.now(clock);

        // Rollback giải phóng tồn giữ chỗ via InventoryService (đảm bảo chuyển StockReservation sang RELEASED)
        boolean stockReleased = inventoryService.releaseStockForOrder(order.getOrderId(), request.getReason().trim());

        // Hoàn trả lượt sử dụng voucher nếu có (chỉ hoàn trả 1 lần bằng conditional CAS)
        if (order.getVoucher() != null) {
            voucherRepository.decrementUsedCountAtomic(order.getVoucher().getVoucherId());
        }

        // Cập nhật trạng thái Payment sang CANCELLED hoặc REFUND_PENDING nếu đã thanh toán
        boolean needRefund = "PAID".equalsIgnoreCase(order.getPaymentStatus()) || PaymentStatus.SUCCESS.name().equalsIgnoreCase(order.getPaymentStatus());
        paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId()).ifPresent(payment -> {
            if (needRefund) {
                payment.setStatus(PaymentStatus.REFUND_PENDING.name());
            } else if (PaymentStatus.PENDING.name().equalsIgnoreCase(payment.getStatus()) || PaymentStatus.UNPAID.name().equalsIgnoreCase(payment.getStatus())) {
                payment.setStatus(PaymentStatus.CANCELLED.name());
            }
            paymentRepository.save(payment);
        });

        // Cập nhật đơn hàng qua OrderStateService
        order.setCancelReason(request.getReason().trim());
        if (needRefund) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING.name());
        }
        orderStateService.transition(order, OrderStatus.CANCELLED, currentUser,
                "Hủy đơn hàng: " + request.getReason().trim() + (needRefund ? " (Đã tạo yêu cầu hoàn tiền REFUND_PENDING)" : ""));
        orderRepository.save(order);

        // Phát Outbox Event OrderCancelled / RefundRequested
        Map<String, Object> cancelPayload = new HashMap<>();
        cancelPayload.put("orderId", order.getOrderId());
        cancelPayload.put("orderCode", order.getOrderCode() != null ? order.getOrderCode() : "");
        cancelPayload.put("cancelReason", request.getReason().trim());
        cancelPayload.put("refundPending", needRefund);
        cancelPayload.put("refundAmount", needRefund ? order.getFinalTotal() : BigDecimal.ZERO);
        cancelPayload.put("refundRequestedAt", DateTimeUtil.formatIsoInstant(now));
        cancelPayload.put("cancelledBy", currentUser.getEmail() != null ? currentUser.getEmail() : "USER");
        outboxService.saveEvent("ORDER",
                order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId()),
                needRefund ? "RefundRequested" : "OrderCancelled",
                "order-cancelled-topic", cancelPayload);

// Lịch sử trạng thái đã được OrderStateService tự động ghi nhận

        // Gửi thông báo hủy đơn cho khách hàng
        if (order.getUser() != null) {
            try {
                notificationRepository.save(Notification.builder()
                        .user(order.getUser())
                        .order(order)
                        .title("Đơn hàng đã bị hủy")
                        .content("Đơn hàng " + order.getOrderCode() + " đã được hủy thành công. Lý do: " + request.getReason().trim())
                        .isRead(false)
                        .build());
            } catch (Exception notifEx) {
                log.warn("Lỗi gửi thông báo hủy đơn: {}", notifEx.getMessage());
            }
        }

        return CancelOrderResponse.builder()
                .orderCode(order.getOrderCode())
                .status(OrderStatus.CANCELLED.name())
                .cancelReason(request.getReason().trim())
                .releasedHoldStock(stockReleased)
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
                                Collectors.mapping(orderMapper::toOrderStatusHistoryResponse, Collectors.toList())
                        ));

        Map<Long, ReturnRequest> adminReturnMap = orderIds.isEmpty() ? Map.of() :
                returnRequestRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(orderIds).stream()
                        .collect(Collectors.toMap(r -> r.getOrder().getOrderId(), r -> r, (r1, r2) -> r1));

        List<AdminOrderItemResponse> items = orders.stream().map(order -> {
            List<OrderItem> orderItems = orderItemsMap.getOrDefault(order.getOrderId(), List.of());
            String itemsSummary = buildItemsSummary(orderItems);
            Shipment shipment = shipmentMap.get(order.getOrderId());
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
                    .items(orderItems.stream().map(orderMapper::toOrderItemResponse).toList())
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
        if (!"WAITING_CONFIRMATION".equalsIgnoreCase(currentStatus)) {
            throw new AppException(ErrorCode.ERR_ORDER_INVALID_STATE,
                    "Chỉ có thể xác nhận đơn hàng đang ở trạng thái Chờ xác nhận (WAITING_CONFIRMATION)!");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        String confirmNote = StringUtils.hasText(request.getNote()) ? request.getNote() : "Duyệt xác nhận đơn hàng MB02 thành công và tạo vận đơn vận chuyển (" + request.getCarrier().trim() + ")";
        orderStateService.transition(order, OrderStatus.CONFIRMED, operator, confirmNote);
        orderRepository.save(order);

        // Sinh mã tracking vận đơn: GHN-YYYYMMDD-OrderId
        String dateStr = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String trackingCode = "GHN-" + dateStr + order.getOrderId();

        BigDecimal codAmt = "COD".equalsIgnoreCase(order.getPaymentMethod()) ? order.getFinalTotal() : BigDecimal.ZERO;

        Shipment shipment = Shipment.builder()
                .order(order)
                .carrierName(request.getCarrier().trim())
                .trackingNumber(trackingCode)
                .shippingStatus(ShipmentStatus.READY_TO_PICK.name())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .codAmount(codAmt)
                .notes(request.getNote())
                .updatedBy(operator)
                .build();
        Shipment savedShipment = shipmentRepository.save(shipment);

// Lịch sử trạng thái đã được OrderStateService tự động ghi nhận

        // Ghi Audit Log
        auditLogRepository.log(operator, "CONFIRM_ORDER", "Orders", order.getOrderId(),
                "status: " + currentStatus, "status: CONFIRMED, tracking: " + trackingCode);

        // Ghi nhận sự kiện xác nhận đơn hàng vào outbox_events để publish Kafka
        Map<String, Object> confirmPayload = new HashMap<>();
        confirmPayload.put("orderId", order.getOrderId());
        confirmPayload.put("orderCode", order.getOrderCode() != null ? order.getOrderCode() : "");
        confirmPayload.put("trackingCode", trackingCode);
        confirmPayload.put("status", "CONFIRMED");
        confirmPayload.put("carrier", request.getCarrier().trim());
        confirmPayload.put("confirmedBy", operator.getFullName() != null ? operator.getFullName() : (operator.getEmail() != null ? operator.getEmail() : "ADMIN"));

        outboxService.saveEvent("ORDER",
                order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getOrderId()),
                "OrderConfirmed",
                "order-confirmed-topic", confirmPayload);

        // Gửi thông báo xác nhận đơn hàng cho khách hàng
        if (order.getUser() != null) {
            notificationRepository.save(Notification.builder()
                    .user(order.getUser())
                    .order(order)
                    .title("Đơn hàng đã được xác nhận")
                    .content("Đơn hàng " + order.getOrderCode() + " đã được xác nhận (MB02) và đang được chuẩn bị để giao cho " + request.getCarrier().trim() + ".")
                    .isRead(false)
                    .build());
        }

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
     * Chuyển trạng thái đơn hàng sang Đang chuẩn bị hàng (PREPARING) & Đẩy sang Vận đơn
     */
    @Transactional
    public OrderDetailResponse prepareOrder(Long id) {
        User operator = securityHelper.authenticateStaffOrAdmin();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + id + "!"));

        String currentStatus = order.getStatus();
        if (!"CONFIRMED".equalsIgnoreCase(currentStatus) && !"PREPARING".equalsIgnoreCase(currentStatus)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng phải ở trạng thái Đã xác nhận (CONFIRMED) để chuyển sang chuẩn bị hàng!");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        orderStateService.transition(order, OrderStatus.PREPARING, operator,
                "Bộ phận kho đang chuẩn bị hàng, kiểm đếm đóng gói và hệ thống đã đẩy sang bảng Vận đơn.");
        orderRepository.save(order);

        // Đảm bảo khởi tạo bản ghi Vận đơn và đẩy sang Quản lý Vận đơn (Shipments)
        Shipment shipment = shipmentRepository.findFirstByOrder_OrderId(order.getOrderId()).orElse(null);
        if (shipment == null) {
            String dateStr = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String trackingCode = "GHN-" + dateStr + order.getOrderId();
            BigDecimal codAmt = "COD".equalsIgnoreCase(order.getPaymentMethod()) ? order.getFinalTotal() : BigDecimal.ZERO;

            shipment = Shipment.builder()
                    .order(order)
                    .carrierName("Giao Hàng Nhanh (GHN)")
                    .trackingNumber(trackingCode)
                    .shippingStatus(ShipmentStatus.READY_TO_PICK.name())
                    .recipientName(order.getRecipientName())
                    .recipientPhone(order.getRecipientPhone())
                    .shippingAddress(order.getShippingAddress())
                    .codAmount(codAmt)
                    .notes("Đơn hàng đang chuẩn bị và đã đẩy sang bộ phận vận đơn")
                    .updatedBy(operator)
                    .build();
            shipmentRepository.save(shipment);
        } else {
            shipment.setShippingStatus(ShipmentStatus.READY_TO_PICK.name());
            shipment.setUpdatedAt(now);
            shipmentRepository.save(shipment);
        }

// Lịch sử trạng thái đã được OrderStateService tự động ghi nhận

        try {
            auditLogRepository.log(operator, "PREPARE_ORDER", "Orders", order.getOrderId(),
                    "status: " + currentStatus, "status: PREPARING");
        } catch (Exception e) {
            log.warn("Không thể ghi audit log prepareOrder: {}", e.getMessage());
        }

        return getOrderDetail(order.getOrderCode());
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
        if (!"CONFIRMED".equalsIgnoreCase(currentStatus) && !"PREPARING".equalsIgnoreCase(currentStatus) && !"PICKED_UP".equalsIgnoreCase(currentStatus)) {
            throw new AppException(ErrorCode.ERR_ORDER_NOT_CONFIRMED,
                    "Đơn hàng phải được duyệt xác nhận (CONFIRMED) trước khi thực hiện xuất kho MB03!");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // Xuất kho vật lý MB03 nguyên tử (trừ tồn kho vật lý và trừ reserved, chuyển reservation sang CONFIRMED)
        List<ExportedItemResponse> exportedItems = inventoryService.exportStockForOrderWithDetails(order.getOrderId(), operator, request.getExportNote());
        if (exportedItems == null || exportedItems.isEmpty()) {
            throw new AppException(ErrorCode.ERR_STOCK_INSUFFICIENT, "Xuất kho không thành công do thiếu tồn kho vật lý!");
        }

        // Cập nhật trạng thái đơn sang SHIPPING qua OrderStateService
        String exportHistoryNote = "Đóng gói & Xuất kho MB03, bàn giao đơn vị vận chuyển" + (StringUtils.hasText(request.getAssignedShipper()) ? " (Tài xế: " + request.getAssignedShipper() + ")" : "");
        orderStateService.transition(order, OrderStatus.SHIPPING, operator, exportHistoryNote);
        orderRepository.save(order);

        // Cập nhật trạng thái vận đơn sang IN_TRANSIT
        BigDecimal fallbackCodAmt = "COD".equalsIgnoreCase(order.getPaymentMethod()) ? order.getFinalTotal() : BigDecimal.ZERO;
        Shipment shipment = shipmentRepository.findFirstByOrder_OrderId(order.getOrderId())
                .orElseGet(() -> Shipment.builder()
                        .order(order)
                        .carrierName("Giao Hàng Nhanh Express")
                        .trackingNumber("GHN-" + order.getOrderId())
                        .recipientName(order.getRecipientName())
                        .recipientPhone(order.getRecipientPhone())
                        .shippingAddress(order.getShippingAddress())
                        .codAmount(fallbackCodAmt)
                        .build());

        shipment.setShippingStatus("IN_TRANSIT");
        shipment.setShippedAt(now);
        shipment.setUpdatedBy(operator);
        if (StringUtils.hasText(request.getExportNote())) {
            shipment.setNotes(request.getExportNote());
        }
        Shipment savedShipment = shipmentRepository.save(shipment);

// Lịch sử trạng thái đã được OrderStateService tự động ghi nhận

        // Ghi Audit Log
        auditLogRepository.log(operator, "PACK_EXPORT_ORDER", "Orders", order.getOrderId(),
                "status: " + currentStatus, "status: SHIPPING, form: MB03");

        // Gửi thông báo đóng gói & xuất kho cho khách hàng
        if (order.getUser() != null) {
            notificationRepository.save(Notification.builder()
                    .user(order.getUser())
                    .order(order)
                    .title("Đơn hàng đang được giao")
                    .content("Kiện hàng " + order.getOrderCode() + " đã xuất kho (MB03) và đang trên đường giao đến bạn.")
                    .isRead(false)
                    .build());
        }

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

        if (!OrderStatus.PENDING_PAYMENT.name().equalsIgnoreCase(order.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Không thể xác nhận thanh toán vì đơn hàng đang ở trạng thái " + order.getStatus() + "!");
        }

        Optional<Payment> paymentOpt = paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId);
        if (paymentOpt.isPresent() && PaymentStatus.SUCCESS.name().equalsIgnoreCase(paymentOpt.get().getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Giao dịch thanh toán này đã được xác nhận thành công trước đó!");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        order.setPaymentStatus("PAID");
        String noteText = StringUtils.hasText(note) ? note.trim() : "Nhân viên xác nhận đã nhận tiền chuyển khoản ngân hàng";
        orderStateService.transition(order, OrderStatus.WAITING_CONFIRMATION, operator, noteText);
        Order updated = orderRepository.save(order);

        // Cập nhật hoặc tạo mới bản ghi Payment với mã giao dịch duy nhất
        String txRef = "TXN-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
        Payment payment = paymentOpt.orElseGet(() -> Payment.builder()
                .order(updated)
                .paymentMethod(updated.getPaymentMethod() != null ? updated.getPaymentMethod() : "BANK_TRANSFER")
                .paymentProvider("SIMULATED_QR")
                .amount(updated.getFinalTotal())
                .build());
        payment.setAmount(updated.getFinalTotal());
        payment.setTransactionRef(txRef);
        payment.setStatus(PaymentStatus.SUCCESS.name());
        payment.setPaidAt(now);
        paymentRepository.save(payment);

// Lịch sử trạng thái đã được OrderStateService tự động ghi nhận

        try {
            auditLogRepository.log(operator, "CONFIRM_PAYMENT", "Orders", orderId, "paymentStatus: UNPAID", "paymentStatus: PAID");
        } catch (Exception e) {
            log.warn("Không thể ghi audit log confirmOrderPayment: {}", e.getMessage());
        }

        outboxService.saveEvent("PAYMENT", updated.getOrderCode(), "PaymentSucceeded",
                "payment-events-topic", Map.of(
                        "orderId", updated.getOrderId(),
                        "orderCode", updated.getOrderCode(),
                        "amount", updated.getFinalTotal(),
                        "paymentMethod", updated.getPaymentMethod(),
                        "confirmedBy", operator.getEmail() != null ? operator.getEmail() : "STAFF"
                ));

        // Khôi phục gửi thông báo xác nhận thanh toán thành công cho khách hàng
        if (updated.getUser() != null) {
            try {
                notificationRepository.save(Notification.builder()
                        .user(updated.getUser())
                        .order(updated)
                        .title("Thanh toán thành công")
                        .content("Đơn hàng " + updated.getOrderCode() + " đã được xác nhận thanh toán thành công số tiền "
                                + CURRENCY_FORMATTER.format(updated.getFinalTotal()) + "đ.")
                        .isRead(false)
                        .build());
            } catch (Exception ex) {
                log.warn("Gửi thông báo thanh toán thành công thất bại: {}", ex.getMessage());
            }
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

        // Chỉ chủ đơn mới được gửi thông báo, không cho staff bypass
        if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ chủ sở hữu đơn hàng mới có thể gửi thông báo chuyển khoản!");
        }

        if (!OrderStatus.PENDING_PAYMENT.name().equalsIgnoreCase(order.getStatus()) || !"BANK_TRANSFER".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Đơn hàng không thuộc diện chờ chuyển khoản ngân hàng!");
        }

        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            order.setPaymentStatus("CUSTOMER_NOTIFIED");
            order.setUpdatedAt(LocalDateTime.now(clock));
            orderRepository.save(order);

            paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(orderId).ifPresent(p -> {
                p.setStatus("CUSTOMER_NOTIFIED");
                paymentRepository.save(p);
            });

            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .changedBy(currentUser)
                    .previousStatus(order.getStatus())
                    .newStatus(order.getStatus())
                    .note("Khách hàng thông báo đã chuyển khoản thành công" + (StringUtils.hasText(note) ? ": " + note.trim() : ""))
                    .build());
        }

        return getOrderDetail(order.getOrderCode());
    }

    /**
     * Tự động quét và hủy các đơn hàng chờ thanh toán QR đã hết hạn (Mỗi đơn chạy trong transaction độc lập)
     */
    public int cancelExpiredOrders() {
        int expireMins = paymentExpireMinutes > 0 ? paymentExpireMinutes : 30;
        LocalDateTime expiredBefore = LocalDateTime.now(clock).minusMinutes(expireMins);
        List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(expiredBefore);
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

    /**
     * API 8.9: [POST] /api/v1/admin/orders/{id}/receive-return — Thủ kho nhận hàng hoàn kho & Hoàn tất hủy đơn (STAFF / ADMIN)
     */
    @Transactional
    public OrderDetailResponse receiveReturnOrder(Long orderId, String note) {
        User operator = securityHelper.authenticateStaffOrAdmin();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_ORDER_NOT_FOUND, "Không tìm thấy đơn hàng ID " + orderId + "!"));

        String currentStatus = order.getStatus();
        if (!OrderStatus.RETURNING.name().equalsIgnoreCase(currentStatus)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Chỉ có thể xác nhận nhận hàng hoàn cho đơn hàng đang ở trạng thái RETURNING (Bưu tá giao không thành công)!");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // Kiểm tra và thực hiện hoàn kho nếu chưa hoàn (MB04) qua InventoryService
        inventoryService.restockReturnOrder(order.getOrderId(), operator, note);

        // Hoàn trả voucher nếu có
        if (order.getVoucher() != null) {
            voucherRepository.decrementUsedCountAtomic(order.getVoucher().getVoucherId());
        }

        boolean needRefund = "PAID".equalsIgnoreCase(order.getPaymentStatus())
                || PaymentStatus.SUCCESS.name().equalsIgnoreCase(order.getPaymentStatus())
                || PaymentStatus.REFUND_PENDING.name().equalsIgnoreCase(order.getPaymentStatus());
        if (needRefund) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING.name());
        } else {
            order.setPaymentStatus(PaymentStatus.CANCELLED.name());
        }
        String finalReason = (note != null && !note.trim().isBlank())
                ? note.trim()
                : (order.getCancelReason() != null ? order.getCancelReason() : "Bưu tá hoàn hàng về kho");
        order.setCancelReason(finalReason);
        orderStateService.transition(order, OrderStatus.CANCELLED, operator, "Xác nhận nhận hàng hoàn về kho (MB04): " + finalReason);
        Order saved = orderRepository.save(order);

        paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(order.getOrderId()).ifPresent(payment -> {
            if (needRefund) {
                payment.setStatus(PaymentStatus.REFUND_PENDING.name());
            } else {
                payment.setStatus(PaymentStatus.CANCELLED.name());
            }
            paymentRepository.save(payment);
        });

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(saved)
                .changedBy(operator)
                .previousStatus(currentStatus)
                .newStatus(OrderStatus.CANCELLED.name())
                .note("Thủ kho đã nhận lại kiện hàng từ bưu tá, nhập lại tồn kho và hoàn tất hủy đơn." +
                        (needRefund ? " (Chờ kế toán hoàn tiền)" : ""))
                .build());

        // Đồng bộ Vận đơn sang FAILED_DELIVERY nếu có
        Optional<Shipment> shipmentOpt = shipmentRepository.findFirstByOrder_OrderId(order.getOrderId());
        if (shipmentOpt.isPresent()) {
            Shipment sh = shipmentOpt.get();
            if (!"FAILED_DELIVERY".equalsIgnoreCase(sh.getShippingStatus())) {
                sh.setShippingStatus("FAILED_DELIVERY");
                sh.setFailReason(finalReason);
                sh.setUpdatedBy(operator);
                shipmentRepository.save(sh);
            }
        }

        // Ghi nhận sự kiện hoàn hàng về kho (MB04) vào outbox_events để publish Kafka
        Map<String, Object> returnPayload = new HashMap<>();
        returnPayload.put("orderId", saved.getOrderId());
        returnPayload.put("orderCode", saved.getOrderCode() != null ? saved.getOrderCode() : "");
        returnPayload.put("status", "CANCELLED");
        returnPayload.put("formCode", "MB04");
        returnPayload.put("receivedBy", operator != null && operator.getFullName() != null ? operator.getFullName() : (operator != null && operator.getEmail() != null ? operator.getEmail() : "ADMIN"));
        returnPayload.put("reason", finalReason);

        outboxService.saveEvent("ORDER",
                saved.getOrderCode() != null ? saved.getOrderCode() : String.valueOf(saved.getOrderId()),
                "ReturnReceivedMB04",
                "order-return-topic", returnPayload);

        if (order.getUser() != null) {
            notificationRepository.save(Notification.builder()
                    .user(order.getUser())
                    .order(saved)
                    .title("Đơn hàng đã hoàn về kho")
                    .content("Kiện hàng của đơn " + order.getOrderCode() + " đã hoàn về kho NextGen và cập nhật Đã hủy.")
                    .isRead(false)
                    .build());
        }

        return getOrderDetail(order.getOrderCode());
    }

    private String resolveDisplayPaymentStatus(Order order) {
        if (order == null) return PaymentStatus.UNPAID.name();
        String currentPayStatus = order.getPaymentStatus();
        if ("PAID".equalsIgnoreCase(currentPayStatus)
                || PaymentStatus.SUCCESS.name().equalsIgnoreCase(currentPayStatus)
                || ("COMPLETED".equalsIgnoreCase(order.getStatus())
                    && !"REFUNDED".equalsIgnoreCase(currentPayStatus)
                    && !"PARTIALLY_REFUNDED".equalsIgnoreCase(currentPayStatus))) {
            return "PAID";
        }
        return currentPayStatus != null ? currentPayStatus : PaymentStatus.UNPAID.name();
    }
}
