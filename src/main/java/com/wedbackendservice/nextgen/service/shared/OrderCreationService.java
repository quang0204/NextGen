package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.request.order.CreateOrderRequest;
import com.wedbackendservice.nextgen.dto.client.response.order.CreateOrderResponse;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.*;
import com.wedbackendservice.nextgen.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final SecurityHelper securityHelper;
    private final TokenRedisService tokenRedisService;
    private final OrderPricingService orderPricingService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final OutboxService outboxService;
    private final OrderPostCommitService orderPostCommitService;
    private final Clock clock;

    @Value("${payment.bank-transfer.expire-minutes:30}")
    private int paymentExpireMinutes = 30;

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
            // 2-7. Tính toán giá, khuyến mãi, voucher và phí ship qua OrderPricingService
            OrderPricingService.PricingResult pricing = orderPricingService.calculatePricing(
                    request.getItems(),
                    request.getVoucherCode(),
                    request.getShippingMethod(),
                    request.getExpectedTotalAmount(),
                    now);

            Map<Long, Integer> consolidatedQty = pricing.getConsolidatedQty();
            List<OrderItem> orderItemsToSave = pricing.getOrderItemsToSave();
            List<OrderCreatedEvent.OrderItemDto> eventItems = pricing.getEventItems();
            BigDecimal subtotal = pricing.getSubtotal();
            Voucher appliedVoucher = pricing.getAppliedVoucher();
            BigDecimal voucherDiscount = pricing.getVoucherDiscount();
            BigDecimal shippingFee = pricing.getShippingFee();
            BigDecimal totalAmount = pricing.getTotalAmount();

            // 8. Validate Payment Method Whitelist (Chỉ COD hoặc BANK_TRANSFER)
            String payMethod = validatePaymentMethod(request.getPaymentMethod());
            boolean isTransfer = PaymentMethod.BANK_TRANSFER.name().equals(payMethod);
            String initialStatus = isTransfer ? OrderStatus.PENDING_PAYMENT.name() : OrderStatus.WAITING_CONFIRMATION.name();

            // 9. Lưu Order với mã đơn hàng duy nhất không đụng độ
            Order savedOrder = saveOrder(currentUser, appliedVoucher, initialStatus, payMethod,
                    request, subtotal, voucherDiscount, shippingFee, totalAmount);

            // 10. Lưu Order Items
            for (OrderItem item : orderItemsToSave) {
                item.setOrder(savedOrder);
                orderItemRepository.save(item);
            }

            // 11. Khởi tạo Payment nếu là BANK_TRANSFER
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

            // 12. GIỮ KHO ĐỒNG BỘ NGUYÊN TỬ
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
                            log.warn("[OrderCreationService] Lưu Redis idempotency key sau commit thất bại: {}", ex.getMessage());
                        }

                        try {
                            orderPostCommitService.cleanupCartAndSendNotification(
                                    currentUser.getUserId(), consolidatedQty.keySet(),
                                    savedOrder.getOrderId(), savedOrder.getOrderCode(),
                                    isTransfer, paymentExpireMinutes);
                        } catch (Exception ex) {
                            log.warn("[OrderCreationService] Post-commit dọn cart & notification thất bại: {}", ex.getMessage());
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        try {
                            if (status != TransactionSynchronization.STATUS_COMMITTED && finalRedisKey != null) {
                                tokenRedisService.deleteKey(finalRedisKey);
                            }
                        } catch (Exception ex) {
                            log.warn("[OrderCreationService] Xóa Redis key sau rollback thất bại: {}", ex.getMessage());
                        }
                    }
                });
            } else {
                try {
                    if (finalRedisKey != null) {
                        tokenRedisService.setValue(finalRedisKey, savedOrder.getOrderCode(), Duration.ofHours(24));
                    }
                } catch (Exception ex) {
                    log.warn("[OrderCreationService] Lưu Redis idempotency key thất bại: {}", ex.getMessage());
                }

                try {
                    orderPostCommitService.cleanupCartAndSendNotification(
                            currentUser.getUserId(), consolidatedQty.keySet(),
                            savedOrder.getOrderId(), savedOrder.getOrderCode(),
                            isTransfer, paymentExpireMinutes);
                } catch (Exception ex) {
                    log.warn("[OrderCreationService] Post-commit dọn cart & notification thất bại: {}", ex.getMessage());
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

    private String validatePaymentMethod(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Phương thức thanh toán không được để trống!");
        }
        String upper = raw.trim().toUpperCase();
        if (!PaymentMethod.COD.name().equals(upper) && !PaymentMethod.BANK_TRANSFER.name().equals(upper)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Phương thức thanh toán '" + raw + "' không được hỗ trợ!");
        }
        return upper;
    }

    private Order saveOrder(User user, Voucher voucher, String status, String payMethod,
                            CreateOrderRequest request, BigDecimal subtotal, BigDecimal discount,
                            BigDecimal shippingFee, BigDecimal total) {
        String today = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyMMdd"));
        String orderCode;
        int maxRetries = 5;
        int retry = 0;
        do {
            int randomNum = ThreadLocalRandom.current().nextInt(10000, 99999);
            orderCode = "DH-" + today + "-" + randomNum;
            retry++;
        } while (orderRepository.existsByOrderCode(orderCode) && retry < maxRetries);

        if (orderRepository.existsByOrderCode(orderCode)) {
            orderCode = "DH-" + today + "-" + ThreadLocalRandom.current().nextInt(10000, 99999) + "-" + (System.currentTimeMillis() % 10000);
        }

        Order order = Order.builder()
                .orderCode(orderCode)
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
}
