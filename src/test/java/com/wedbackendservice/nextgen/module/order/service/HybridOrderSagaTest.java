package com.wedbackendservice.nextgen.module.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.core.config.BankTransferConfig;
import com.wedbackendservice.nextgen.core.event.saga.KafkaTopics;
import com.wedbackendservice.nextgen.core.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.core.event.saga.PaymentResultEvent;
import com.wedbackendservice.nextgen.core.event.saga.StockReservedEvent;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.listener.saga.CartEventListener;
import com.wedbackendservice.nextgen.core.listener.saga.InventorySagaListener;
import com.wedbackendservice.nextgen.core.listener.saga.NotificationEventListener;
import com.wedbackendservice.nextgen.core.listener.saga.OrderSagaListener;
import com.wedbackendservice.nextgen.core.listener.saga.PaymentSagaListener;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.module.inventory.service.InventoryService;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.notification.service.InboxService;
import com.wedbackendservice.nextgen.module.order.dto.client.request.CreateOrderRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.request.OrderItemRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.response.CreateOrderResponse;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderItem;
import com.wedbackendservice.nextgen.module.order.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.port.out.cart.CartPort;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryPort;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryUnavailableException;
import com.wedbackendservice.nextgen.module.order.port.out.notification.NotificationPort;
import com.wedbackendservice.nextgen.module.order.port.out.payment.PaymentPort;
import com.wedbackendservice.nextgen.module.order.port.out.shipment.ShipmentPort;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.payment.entity.Payment;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentMethod;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import com.wedbackendservice.nextgen.module.payment.service.client.PaymentService;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HybridOrderSagaTest {

  @Mock private SecurityHelper securityHelper;
  @Mock private TokenRedisService tokenRedisService;
  @Mock private OrderPricingService orderPricingService;
  @Mock private OrderRepository orderRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
  @Mock private PaymentPort paymentPort;
  @Mock private InventoryPort inventoryPort;
  @Mock private OutboxService outboxService;
  @Mock private CartPort cartPort;
  @Mock private NotificationPort notificationPort;
  @Mock private ShipmentPort shipmentPort;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private OrdersQueryService ordersQueryService;

  @Mock private InventoryService inventoryService;
  @Mock private PaymentRepository paymentRepository;
  @Mock private InboxService inboxService;
  @Mock private BankTransferConfig bankTransferConfig;
  @Mock private OrderStateService orderStateService;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final Clock fixedClock =
      Clock.fixed(Instant.parse("2026-11-25T04:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

  private OrderCreationService orderCreationService;
  private OrderFulfillmentService orderFulfillmentService;
  private InventorySagaListener inventorySagaListener;
  private PaymentSagaListener paymentSagaListener;
  private OrderSagaListener orderSagaListener;
  private CartEventListener cartEventListener;
  private NotificationEventListener notificationEventListener;
  private PaymentService paymentService;
  private User customer;

  @BeforeEach
  void setUp() {
    orderCreationService =
        new OrderCreationService(
            securityHelper,
            tokenRedisService,
            orderPricingService,
            orderRepository,
            orderItemRepository,
            orderStatusHistoryRepository,
            paymentPort,
            inventoryPort,
            outboxService,
            fixedClock);
    orderFulfillmentService =
        new OrderFulfillmentService(
            securityHelper,
            orderRepository,
            shipmentPort,
            paymentPort,
            notificationPort,
            auditLogRepository,
            orderStateService,
            inventoryPort,
            ordersQueryService,
            outboxService,
            fixedClock);
    inventorySagaListener =
        new InventorySagaListener(inventoryService, outboxService, inboxService, objectMapper);
    paymentSagaListener =
        new PaymentSagaListener(
            paymentRepository, orderRepository, outboxService, inboxService, objectMapper);
    orderSagaListener =
        new OrderSagaListener(
            orderRepository,
            orderStatusHistoryRepository,
            outboxService,
            inboxService,
            objectMapper);
    cartEventListener = new CartEventListener(cartPort, inboxService, objectMapper);
    notificationEventListener =
        new NotificationEventListener(
            notificationPort, orderRepository, inboxService, objectMapper);
    paymentService =
        new PaymentService(
            paymentRepository,
            orderRepository,
            orderStatusHistoryRepository,
            securityHelper,
            bankTransferConfig,
            inventoryService,
            outboxService,
            orderStateService);

    customer = User.builder().userId(1L).email("khach@nextgen.vn").role(Role.CUSTOMER).build();
  }

  private OrderPricingService.PricingResult samplePricing() {
    ProductVariant variant = ProductVariant.builder().variantId(101L).sku("SKU-101").build();
    OrderItem item =
        OrderItem.builder()
            .variant(variant)
            .price(BigDecimal.valueOf(1000000))
            .quantity(2)
            .totalPrice(BigDecimal.valueOf(2000000))
            .build();
    OrderCreatedEvent.OrderItemDto dto =
        OrderCreatedEvent.OrderItemDto.builder()
            .variantId(101L)
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(1000000))
            .build();
    return OrderPricingService.PricingResult.builder()
        .consolidatedQty(Map.of(101L, 2))
        .variantMap(Map.of(101L, variant))
        .orderItemsToSave(List.of(item))
        .eventItems(List.of(dto))
        .subtotal(BigDecimal.valueOf(2000000))
        .voucherDiscount(BigDecimal.ZERO)
        .shippingFee(BigDecimal.ZERO)
        .totalAmount(BigDecimal.valueOf(2000000))
        .build();
  }

  private CreateOrderRequest sampleRequest(String paymentMethod) {
    return CreateOrderRequest.builder()
        .recipientName("Nguyen Van A")
        .recipientPhone("0987654321")
        .shippingAddress("123 Le Loi, HCM")
        .paymentMethod(paymentMethod)
        .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(2).build()))
        .build();
  }

  @Test
  void createOrder_syncCriticalPathCod_createsSinglePaymentAndMarksSyncCompleted() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(inventoryPort.isReserveAvailable()).thenReturn(true);
    when(orderPricingService.calculatePricing(anyList(), any(), any(), any(), any()))
        .thenReturn(samplePricing());
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            inv -> {
              Order o = inv.getArgument(0);
              o.setOrderId(501L);
              return o;
            });
    when(inventoryPort.reserveStockForOrder(eq(501L), anyList(), eq(0))).thenReturn(true);

    CreateOrderResponse response = orderCreationService.createOrder(sampleRequest("COD"), null);

    assertThat(response.getStatus()).isEqualTo(OrderStatus.WAITING_CONFIRMATION.name());
    verify(paymentPort)
        .createInitialPayment(
            any(Order.class), eq(BigDecimal.valueOf(2000000)), eq("COD"), eq("COD_SHIPPER"));

    ArgumentCaptor<OrderCreatedEvent> eventCaptor =
        ArgumentCaptor.forClass(OrderCreatedEvent.class);
    verify(outboxService)
        .saveEvent(
            eq("ORDER"),
            anyString(),
            eq("OrderCreated"),
            eq(KafkaTopics.ORDER_CREATED),
            eventCaptor.capture());
    assertThat(eventCaptor.getValue().isSyncCompleted()).isTrue();
    assertThat(eventCaptor.getValue().getHoldMinutes()).isEqualTo(0);
  }

  @Test
  void createOrder_whenInventoryUnavailable_degradesToPendingOrderAndAsyncKafkaSaga() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(inventoryPort.isReserveAvailable()).thenReturn(true);
    when(orderPricingService.calculatePricing(anyList(), any(), any(), any(), any()))
        .thenReturn(samplePricing());
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            inv -> {
              Order o = inv.getArgument(0);
              o.setOrderId(601L);
              return o;
            });
    when(inventoryPort.reserveStockForOrder(eq(601L), anyList(), eq(30)))
        .thenThrow(new InventoryUnavailableException("Timeout kho"));

    CreateOrderResponse response =
        orderCreationService.createOrder(sampleRequest("BANK_TRANSFER"), null);

    assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING.name());
    verify(paymentPort, never()).createInitialPayment(any(), any(), any(), any());

    ArgumentCaptor<OrderCreatedEvent> eventCaptor =
        ArgumentCaptor.forClass(OrderCreatedEvent.class);
    verify(outboxService)
        .saveEvent(
            eq("ORDER"),
            anyString(),
            eq("OrderCreated"),
            eq(KafkaTopics.ORDER_CREATED),
            eventCaptor.capture());
    assertThat(eventCaptor.getValue().isSyncCompleted()).isFalse();
    assertThat(eventCaptor.getValue().getHoldMinutes()).isEqualTo(30);
  }

  @Test
  void createOrder_whenCircuitBreakerOpen_skipsAttemptOneAndCreatesPendingImmediately() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(inventoryPort.isReserveAvailable()).thenReturn(false);
    when(orderPricingService.calculatePricing(anyList(), any(), any(), any(), any()))
        .thenReturn(samplePricing());
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            inv -> {
              Order o = inv.getArgument(0);
              o.setOrderId(602L);
              return o;
            });

    CreateOrderResponse response = orderCreationService.createOrder(sampleRequest("COD"), null);

    assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING.name());
    verify(inventoryPort, never()).reserveStockForOrder(any(), any(), anyInt());
    verify(paymentPort, never()).createInitialPayment(any(), any(), any(), any());

    ArgumentCaptor<OrderCreatedEvent> eventCaptor =
        ArgumentCaptor.forClass(OrderCreatedEvent.class);
    verify(outboxService)
        .saveEvent(
            eq("ORDER"),
            anyString(),
            eq("OrderCreated"),
            eq(KafkaTopics.ORDER_CREATED),
            eventCaptor.capture());
    assertThat(eventCaptor.getValue().isSyncCompleted()).isFalse();
    assertThat(eventCaptor.getValue().getHoldMinutes()).isEqualTo(0);
  }

  @Test
  void createOrder_whenOutOfStock_throwsWithoutCreatingPayment() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(inventoryPort.isReserveAvailable()).thenReturn(true);
    when(orderPricingService.calculatePricing(anyList(), any(), any(), any(), any()))
        .thenReturn(samplePricing());
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            inv -> {
              Order o = inv.getArgument(0);
              o.setOrderId(603L);
              return o;
            });
    when(inventoryPort.reserveStockForOrder(eq(603L), anyList(), eq(0))).thenReturn(false);

    assertThatThrownBy(() -> orderCreationService.createOrder(sampleRequest("COD"), null))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_STOCK_INSUFFICIENT);

    verify(paymentPort, never()).createInitialPayment(any(), any(), any(), any());
    verify(outboxService, never()).saveEvent(any(), any(), any(), any(), any());
  }

  @Test
  void inventorySagaListener_skipsRedundantReservationWhenSyncCompletedTrue() throws Exception {
    OrderCreatedEvent event =
        OrderCreatedEvent.builder()
            .orderId(701L)
            .orderCode("DH-701")
            .paymentMethod("COD")
            .syncCompleted(true)
            .holdMinutes(0)
            .items(List.of())
            .build();
    when(inboxService.isAlreadyProcessed(event.getEventId(), "nextgen-inventory-saga"))
        .thenReturn(false);

    inventorySagaListener.handleOrderCreated(objectMapper.writeValueAsString(event));

    verify(inventoryService, never()).reserveStockForOrder(any(), any(), anyInt());
    verify(outboxService, never()).saveEvent(any(), any(), any(), any(), any());
    verify(inboxService)
        .markAsProcessed(
            event.getEventId(), "nextgen-inventory-saga", "OrderCreatedSync", "DH-701");
  }

  @Test
  void inventorySagaListener_runsReservationWithEventHoldMinutesOnAsyncFallback() throws Exception {
    OrderCreatedEvent event =
        OrderCreatedEvent.builder()
            .orderId(702L)
            .orderCode("DH-702")
            .paymentMethod("COD")
            .totalAmount(BigDecimal.valueOf(500000))
            .syncCompleted(false)
            .holdMinutes(0)
            .items(
                List.of(
                    OrderCreatedEvent.OrderItemDto.builder().variantId(101L).quantity(1).build()))
            .build();
    when(inboxService.isAlreadyProcessed(event.getEventId(), "nextgen-inventory-saga"))
        .thenReturn(false);
    when(inventoryService.reserveStockForOrder(eq(702L), anyList(), eq(0))).thenReturn(true);

    inventorySagaListener.handleOrderCreated(objectMapper.writeValueAsString(event));

    verify(inventoryService).reserveStockForOrder(eq(702L), anyList(), eq(0));
    verify(outboxService)
        .saveEvent(
            eq("INVENTORY"),
            eq("DH-702"),
            eq("StockReserved"),
            eq(KafkaTopics.STOCK_RESERVED),
            any(StockReservedEvent.class));
  }

  @Test
  void paymentSagaListener_idempotent_skipsDuplicatePaymentAndEmitsForBankTransfer()
      throws Exception {
    Order order =
        Order.builder()
            .orderId(801L)
            .orderCode("DH-801")
            .status(OrderStatus.PENDING.name())
            .paymentMethod("BANK_TRANSFER")
            .build();
    StockReservedEvent event =
        StockReservedEvent.builder()
            .orderId(801L)
            .orderCode("DH-801")
            .paymentMethod("BANK_TRANSFER")
            .totalAmount(BigDecimal.valueOf(1000000))
            .success(true)
            .items(List.of())
            .build();

    when(inboxService.isAlreadyProcessed(event.getEventId(), "nextgen-payment-saga"))
        .thenReturn(false);
    when(orderRepository.findById(801L)).thenReturn(Optional.of(order));
    when(paymentRepository.existsByOrder_OrderId(801L)).thenReturn(true);

    paymentSagaListener.handleStockReserved(objectMapper.writeValueAsString(event));

    verify(paymentRepository, never()).save(any(Payment.class));
    verify(outboxService)
        .saveEvent(
            eq("PAYMENT"),
            eq("DH-801"),
            eq("PaymentSucceeded"),
            eq(KafkaTopics.PAYMENT_EVENTS),
            any(PaymentResultEvent.class));
  }

  @Test
  void orderSagaListener_transitionsPendingOrderOnce_andSkipsWhenAlreadyFinalized()
      throws Exception {
    Order pendingOrder =
        Order.builder()
            .orderId(901L)
            .orderCode("DH-901")
            .status(OrderStatus.PENDING.name())
            .paymentStatus(PaymentStatus.UNPAID.name())
            .build();
    PaymentResultEvent event =
        PaymentResultEvent.builder()
            .orderId(901L)
            .orderCode("DH-901")
            .paymentMethod("BANK_TRANSFER")
            .success(true)
            .build();

    when(inboxService.isAlreadyProcessed(event.getEventId(), "nextgen-order-saga"))
        .thenReturn(false);
    when(orderRepository.findById(901L)).thenReturn(Optional.of(pendingOrder));

    orderSagaListener.handlePaymentResult(objectMapper.writeValueAsString(event));

    assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT.name());
    verify(orderRepository).save(pendingOrder);
    verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));

    // Lần gọi với đơn đã không còn PENDING (WAITING_CONFIRMATION): không tạo history trùng
    Order alreadyFinalized =
        Order.builder()
            .orderId(902L)
            .orderCode("DH-902")
            .status(OrderStatus.WAITING_CONFIRMATION.name())
            .paymentStatus(PaymentStatus.UNPAID.name())
            .build();
    PaymentResultEvent dupEvent =
        PaymentResultEvent.builder()
            .orderId(902L)
            .orderCode("DH-902")
            .paymentMethod("COD")
            .success(true)
            .build();
    when(inboxService.isAlreadyProcessed(dupEvent.getEventId(), "nextgen-order-saga"))
        .thenReturn(false);
    when(orderRepository.findById(902L)).thenReturn(Optional.of(alreadyFinalized));

    orderSagaListener.handlePaymentResult(objectMapper.writeValueAsString(dupEvent));

    verify(orderRepository, never()).save(alreadyFinalized);
  }

  @Test
  void paymentService_createPaymentIntent_rejectsPendingAsyncOrder() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    Order pendingOrder =
        Order.builder()
            .orderId(950L)
            .orderCode("DH-950")
            .user(customer)
            .status(OrderStatus.PENDING.name())
            .paymentStatus(PaymentStatus.UNPAID.name())
            .build();
    when(orderRepository.findById(950L)).thenReturn(Optional.of(pendingOrder));

    var req =
        com.wedbackendservice.nextgen.module.payment.dto.client.request.CreatePaymentIntentRequest
            .builder()
            .orderId(950L)
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .build();

    assertThatThrownBy(() -> paymentService.createPaymentIntent(req))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ORDER_PROCESSING);
  }

  @Test
  void cartAndNotificationListeners_processOrderCreatedAndCancelledAsynchronouslyWithInbox()
      throws Exception {
    OrderCreatedEvent createdEvent =
        OrderCreatedEvent.builder()
            .orderId(960L)
            .orderCode("DH-960")
            .userId(1L)
            .paymentMethod("BANK_TRANSFER")
            .holdMinutes(30)
            .syncCompleted(true)
            .items(
                List.of(
                    OrderCreatedEvent.OrderItemDto.builder()
                        .variantId(101L)
                        .quantity(2)
                        .build()))
            .build();
    String createdPayload = objectMapper.writeValueAsString(createdEvent);

    when(inboxService.isAlreadyProcessed(createdEvent.getEventId(), "nextgen-cart-cleanup"))
        .thenReturn(false);
    when(inboxService.isAlreadyProcessed(createdEvent.getEventId(), "nextgen-notification-events"))
        .thenReturn(false);

    cartEventListener.handleOrderCreated(createdPayload);
    notificationEventListener.handleOrderCreated(createdPayload);

    verify(cartPort).clearCartItems(eq(1L), argThat(ids -> ids.contains(101L)));
    verify(notificationPort)
        .sendNotification(
            eq(1L), eq(960L), eq("Đơn hàng đang chờ thanh toán"), contains("30 phút"));
    verify(inboxService)
        .markAsProcessed(
            createdEvent.getEventId(), "nextgen-cart-cleanup", "OrderCreatedCartCleanup", "DH-960");
    verify(inboxService)
        .markAsProcessed(
            createdEvent.getEventId(),
            "nextgen-notification-events",
            "OrderCreatedNotification",
            "DH-960");

    String cancelPayload =
        objectMapper.writeValueAsString(
            Map.of(
                "eventId", "evt-cancel-1",
                "orderId", 960L,
                "orderCode", "DH-960",
                "userId", 1L,
                "cancelReason", "Hết hạn QR"));
    when(inboxService.isAlreadyProcessed("evt-cancel-1", "nextgen-notification-events"))
        .thenReturn(false);

    notificationEventListener.handleOrderCancelled(cancelPayload);

    verify(notificationPort)
        .sendNotification(eq(1L), eq(960L), eq("Đơn hàng đã bị hủy"), contains("Hết hạn QR"));
  }

  @Test
  void confirmOrderPayment_emitsPaymentResultEventWithSuccessTrue() {
    User staff = User.builder().userId(2L).email("staff@nextgen.vn").role(Role.STAFF).build();
    Order order =
        Order.builder()
            .orderId(970L)
            .orderCode("DH-970")
            .user(customer)
            .status(OrderStatus.PENDING_PAYMENT.name())
            .paymentStatus("UNPAID")
            .finalAmount(BigDecimal.valueOf(1500000))
            .build();
    when(securityHelper.getCurrentUser()).thenReturn(staff);
    when(orderRepository.findByIdForUpdate(970L)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    orderFulfillmentService.confirmOrderPayment(970L, "Đã nhận tiền");

    ArgumentCaptor<PaymentResultEvent> captor = ArgumentCaptor.forClass(PaymentResultEvent.class);
    verify(outboxService)
        .saveEvent(
            eq("PAYMENT"),
            eq("DH-970"),
            eq("PaymentSucceeded"),
            eq(KafkaTopics.PAYMENT_EVENTS),
            captor.capture());
    assertThat(captor.getValue().isSuccess()).isTrue();
    assertThat(captor.getValue().getEventId()).isNotBlank();
  }
}
