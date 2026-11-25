package com.wedbackendservice.nextgen.module.order.service;

import com.wedbackendservice.nextgen.core.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.core.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.core.util.DateTimeUtil;
import com.wedbackendservice.nextgen.module.order.dto.client.request.CreateOrderRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.response.CreateOrderResponse;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderItem;
import com.wedbackendservice.nextgen.module.order.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryPort;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryUnavailableException;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.StockItemDto;
import com.wedbackendservice.nextgen.module.order.port.out.payment.PaymentPort;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentMethod;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.voucher.entity.Voucher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

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
  private final PaymentPort paymentPort;
  private final InventoryPort inventoryPort;
  private final OutboxService outboxService;
  private final Clock clock;

  @Autowired(required = false)
  private TransactionTemplate transactionTemplate;

  @Value("${payment.bank-transfer.expire-minutes:30}")
  private int paymentExpireMinutes = 30;

  private record CheckoutAttemptResult(
      CreateOrderResponse response,
      Order savedOrder,
      Set<Long> variantIds,
      boolean asyncFallback) {}

  public CreateOrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
    User currentUser = securityHelper.getCurrentUser();
    LocalDateTime now = LocalDateTime.now(clock);

    // 1. Kiểm tra Idempotency key bằng SETNX chống race condition
    String redisKey = null;
    if (StringUtils.hasText(idempotencyKey)) {
      redisKey = "idempotency:order:" + currentUser.getUserId() + ":" + idempotencyKey.trim();
      boolean acquired =
          tokenRedisService.setIfAbsent(redisKey, "PROCESSING", Duration.ofMinutes(2));
      if (!acquired) {
        String existingCode = tokenRedisService.getValue(redisKey);
        if ("PROCESSING".equals(existingCode)) {
          throw new AppException(
              ErrorCode.ERR_ORDER_PROCESSING,
              "Yêu cầu tạo đơn hàng đang được xử lý, vui lòng chờ trong giây lát!");
        }
        if (StringUtils.hasText(existingCode)) {
          Optional<Order> existing = orderRepository.findByOrderCode(existingCode);
          if (existing.isPresent()) {
            List<OrderItem> existingItems =
                orderItemRepository.findByOrderId(existing.get().getOrderId());
            return toCreateOrderResponse(
                existing.get(), existingItems != null ? existingItems.size() : 0);
          }
        }
        throw new AppException(
            ErrorCode.ERR_ORDER_PROCESSING,
            "Yêu cầu tạo đơn hàng đang được xử lý hoặc đã hoàn tất!");
      }
    }
    final String finalRedisKey = redisKey;

    try {
      String payMethod = validatePaymentMethod(request.getPaymentMethod());
      boolean isTransfer = PaymentMethod.BANK_TRANSFER.name().equals(payMethod);
      int holdMinutes = isTransfer ? paymentExpireMinutes : 0;

      CheckoutAttemptResult result;
      if (inventoryPort.isReserveAvailable()) {
        try {
          // Critical Path đồng bộ: giữ kho + khởi tạo thanh toán trong 1 transaction ngắn gọn
          result =
              runInTransaction(
                  () ->
                      executeCheckoutAttempt(
                          request, currentUser, payMethod, isTransfer, holdMinutes, now, false));
        } catch (InventoryUnavailableException ex) {
          log.warn(
              "[OrderCreationService] Tồn kho quá tải/gián đoạn ({}), hạ cấp sang nhánh Kafka"
                  + " Saga bất đồng bộ!",
              ex.getMessage());
          // Transaction đồng bộ đã rollback sạch sẽ; mở transaction độc lập để tiếp nhận PENDING
          result =
              runInTransaction(
                  () ->
                      executeCheckoutAttempt(
                          request, currentUser, payMethod, isTransfer, holdMinutes, now, true));
        }
      } else {
        log.warn(
            "[OrderCreationService] Circuit Breaker tồn kho đang OPEN, bỏ qua gọi đồng bộ và"
                + " tiếp nhận đơn qua Kafka Saga!");
        result =
            runInTransaction(
                () ->
                    executeCheckoutAttempt(
                        request, currentUser, payMethod, isTransfer, holdMinutes, now, true));
      }

      afterOrderCommitted(finalRedisKey, result.savedOrder());
      return result.response();
    } catch (Exception ex) {
      if (finalRedisKey != null) {
        try {
          tokenRedisService.deleteKey(finalRedisKey);
        } catch (Exception redisEx) {
          log.warn(
              "[OrderCreationService] Rollback xóa Redis key {} thất bại: {}",
              finalRedisKey,
              redisEx.getMessage());
        }
      }
      throw ex;
    }
  }

  private CheckoutAttemptResult executeCheckoutAttempt(
      CreateOrderRequest request,
      User currentUser,
      String payMethod,
      boolean isTransfer,
      int holdMinutes,
      LocalDateTime now,
      boolean asyncFallback) {
    // 2-7. Tính toán giá, khuyến mãi, voucher và phí ship qua OrderPricingService
    OrderPricingService.PricingResult pricing =
        orderPricingService.calculatePricing(
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

    // 8. Xác định trạng thái khởi tạo (PENDING nếu hạ cấp sang Async Kafka Saga)
    String initialStatus =
        asyncFallback
            ? OrderStatus.PENDING.name()
            : (isTransfer
                ? OrderStatus.PENDING_PAYMENT.name()
                : OrderStatus.WAITING_CONFIRMATION.name());

    // 9. Lưu Order với mã đơn hàng duy nhất không đụng độ
    Order savedOrder =
        saveOrder(
            currentUser,
            appliedVoucher,
            initialStatus,
            payMethod,
            request,
            subtotal,
            voucherDiscount,
            shippingFee,
            totalAmount);

    // 10. Lưu Order Items
    for (OrderItem item : orderItemsToSave) {
      item.setOrder(savedOrder);
      orderItemRepository.save(item);
    }

    if (!asyncFallback) {
      // 11. GIỮ KHO ĐỒNG BỘ NGUYÊN TỬ via InventoryPort (lỗi nghiệp vụ hết hàng -> rollback ngay)
      List<StockItemDto> stockItems =
          eventItems.stream()
              .map(
                  i ->
                      StockItemDto.builder()
                          .variantId(i.getVariantId())
                          .quantity(i.getQuantity())
                          .build())
              .toList();
      boolean stockReserved =
          inventoryPort.reserveStockForOrder(savedOrder.getOrderId(), stockItems, holdMinutes);
      if (!stockReserved) {
        throw new AppException(
            ErrorCode.ERR_STOCK_INSUFFICIENT,
            "Một số sản phẩm trong đơn hàng vừa hết hàng hoặc không đủ số lượng khả dụng!");
      }

      // 12. Khởi tạo Payment đồng bộ (1 bản ghi duy nhất cho cả BANK_TRANSFER lẫn COD)
      paymentPort.createInitialPayment(
          savedOrder, totalAmount, payMethod, isTransfer ? "VIETQR_DEMO" : "COD_SHIPPER");
    }

    // 13. Ghi Outbox Event: OrderCreated (Kafka fan-out sau commit, hoặc chạy Saga nếu
    // asyncFallback)
    OrderCreatedEvent orderCreatedEvent =
        OrderCreatedEvent.builder()
            .orderId(savedOrder.getOrderId())
            .orderCode(savedOrder.getOrderCode())
            .userId(currentUser.getUserId())
            .recipientName(savedOrder.getRecipientName())
            .recipientPhone(savedOrder.getRecipientPhone())
            .shippingAddress(savedOrder.getShippingAddress())
            .paymentMethod(payMethod)
            .totalAmount(totalAmount)
            .holdMinutes(holdMinutes)
            .syncCompleted(!asyncFallback)
            .items(eventItems)
            .build();

    outboxService.saveEvent(
        "ORDER",
        savedOrder.getOrderCode(),
        "OrderCreated",
        KafkaTopics.ORDER_CREATED,
        orderCreatedEvent);

    // 14. Ghi Order Status History
    String historyNote =
        asyncFallback
            ? "Tiếp nhận đơn hàng PENDING (chế độ dự phòng bất đồng bộ do kho đang bận, chờ Kafka"
                + " Saga giữ chỗ)."
            : "Khởi tạo đơn hàng "
                + initialStatus
                + " thành công. Đã khóa giữ chỗ tồn kho an toàn.";
    orderStatusHistoryRepository.save(
        OrderStatusHistory.builder()
            .order(savedOrder)
            .changedBy(currentUser)
            .previousStatus(null)
            .newStatus(initialStatus)
            .note(historyNote)
            .build());

    return new CheckoutAttemptResult(
        toCreateOrderResponse(savedOrder, orderItemsToSave.size()),
        savedOrder,
        consolidatedQty.keySet(),
        asyncFallback);
  }

  private <T> T runInTransaction(Supplier<T> action) {
    if (transactionTemplate != null) {
      return transactionTemplate.execute(status -> action.get());
    }
    return action.get();
  }

  private void afterOrderCommitted(String finalRedisKey, Order savedOrder) {
    try {
      if (finalRedisKey != null) {
        tokenRedisService.setValue(finalRedisKey, savedOrder.getOrderCode(), Duration.ofHours(24));
      }
    } catch (Exception ex) {
      log.warn(
          "[OrderCreationService] Lưu Redis idempotency key sau commit thất bại: {}",
          ex.getMessage());
    }
  }

  private String validatePaymentMethod(String raw) {
    if (!StringUtils.hasText(raw)) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST, "Phương thức thanh toán không được để trống!");
    }
    String upper = raw.trim().toUpperCase();
    if ("SIMULATED_TRANSFER".equals(upper)) {
      return PaymentMethod.BANK_TRANSFER.name();
    }
    if (!PaymentMethod.COD.name().equals(upper)
        && !PaymentMethod.BANK_TRANSFER.name().equals(upper)) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST, "Phương thức thanh toán '" + raw + "' không được hỗ trợ!");
    }
    return upper;
  }

  private Order saveOrder(
      User user,
      Voucher voucher,
      String status,
      String payMethod,
      CreateOrderRequest request,
      BigDecimal subtotal,
      BigDecimal discount,
      BigDecimal shippingFee,
      BigDecimal total) {
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
      orderCode =
          "DH-"
              + today
              + "-"
              + ThreadLocalRandom.current().nextInt(10000, 99999)
              + "-"
              + (System.currentTimeMillis() % 10000);
    }

    Order order =
        Order.builder()
            .orderCode(orderCode)
            .user(user)
            .voucher(voucher)
            .status(status)
            .paymentMethod(payMethod)
            .paymentStatus(PaymentStatus.UNPAID.name())
            .recipientName(
                request.getRecipientName() != null ? request.getRecipientName().trim() : "")
            .recipientPhone(
                request.getRecipientPhone() != null ? request.getRecipientPhone().trim() : "")
            .shippingAddress(
                request.getShippingAddress() != null ? request.getShippingAddress().trim() : "")
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
