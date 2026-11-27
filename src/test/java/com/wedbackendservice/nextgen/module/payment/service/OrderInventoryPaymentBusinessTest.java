package com.wedbackendservice.nextgen.module.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.core.config.BankTransferConfig;
import com.wedbackendservice.nextgen.core.event.saga.OrderCreatedEvent;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.module.cart.repository.CartItemRepository;
import com.wedbackendservice.nextgen.module.cart.repository.CartRepository;
import com.wedbackendservice.nextgen.module.inventory.entity.Inventory;
import com.wedbackendservice.nextgen.module.inventory.entity.StockReservation;
import com.wedbackendservice.nextgen.module.inventory.enums.ReservationStatus;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryTransactionRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.StockReservationRepository;
import com.wedbackendservice.nextgen.module.inventory.service.InventoryService;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.notification.repository.InboxEventRepository;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.notification.service.InboxService;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.CartPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.InventoryPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.NotificationPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.PaymentPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.ProductCatalogPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.ShipmentPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.VoucherPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.dto.client.request.CancelOrderRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.request.CreateOrderRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.request.OrderItemRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.response.CancelOrderResponse;
import com.wedbackendservice.nextgen.module.order.dto.client.response.CreateOrderResponse;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderItem;
import com.wedbackendservice.nextgen.module.order.enums.OrderStatus;
import com.wedbackendservice.nextgen.module.order.mapper.OrderMapper;
import com.wedbackendservice.nextgen.module.order.port.out.cart.CartPort;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryPort;
import com.wedbackendservice.nextgen.module.order.port.out.notification.NotificationPort;
import com.wedbackendservice.nextgen.module.order.port.out.payment.PaymentPort;
import com.wedbackendservice.nextgen.module.order.port.out.product.ProductCatalogPort;
import com.wedbackendservice.nextgen.module.order.port.out.shipment.ShipmentPort;
import com.wedbackendservice.nextgen.module.order.port.out.voucher.VoucherPort;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.order.service.OrderCancellationService;
import com.wedbackendservice.nextgen.module.order.service.OrderCreationService;
import com.wedbackendservice.nextgen.module.order.service.OrderExpirationService;
import com.wedbackendservice.nextgen.module.order.service.OrderFulfillmentService;
import com.wedbackendservice.nextgen.module.order.service.OrderPricingService;
import com.wedbackendservice.nextgen.module.order.service.OrderReturnService;
import com.wedbackendservice.nextgen.module.order.service.OrderStateService;
import com.wedbackendservice.nextgen.module.order.service.OrdersQueryService;
import com.wedbackendservice.nextgen.module.order.service.OrdersService;
import com.wedbackendservice.nextgen.module.payment.dto.client.request.CreatePaymentIntentRequest;
import com.wedbackendservice.nextgen.module.payment.dto.client.response.PaymentIntentResponse;
import com.wedbackendservice.nextgen.module.payment.dto.client.response.SimulateCallbackResponse;
import com.wedbackendservice.nextgen.module.payment.entity.Payment;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentMethod;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import com.wedbackendservice.nextgen.module.payment.service.client.PaymentService;
import com.wedbackendservice.nextgen.module.product.entity.Product;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.product.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.request.ShipmentStatusUpdateRequest;
import com.wedbackendservice.nextgen.module.shipment.entity.Shipment;
import com.wedbackendservice.nextgen.module.shipment.repository.ShipmentRepository;
import com.wedbackendservice.nextgen.module.shipment.service.ShipmentsService;
import com.wedbackendservice.nextgen.module.shipment.service.ShippingFeeCalculator;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.voucher.entity.PromotionVariant;
import com.wedbackendservice.nextgen.module.voucher.entity.Voucher;
import com.wedbackendservice.nextgen.module.voucher.repository.PromotionVariantRepository;
import com.wedbackendservice.nextgen.module.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class OrderInventoryPaymentBusinessTest {

  // Repositories
  @Mock private OrderRepository orderRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
  @Mock private InventoryRepository inventoryRepository;
  @Mock private InventoryTransactionRepository inventoryTransactionRepository;
  @Mock private ProductVariantRepository productVariantRepository;
  @Mock private VoucherRepository voucherRepository;
  @Mock private PromotionVariantRepository promotionVariantRepository;
  @Mock private ShipmentRepository shipmentRepository;
  @Mock private CartRepository cartRepository;
  @Mock private CartItemRepository cartItemRepository;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private TokenRedisService tokenRedisService;
  @Mock private PaymentRepository paymentRepository;
  @Mock private NotificationRepository notificationRepository;
  @Mock private StockReservationRepository stockReservationRepository;
  @Mock private InboxEventRepository inboxEventRepository;
  @Mock private BankTransferConfig bankTransferConfig;
  @Mock private SecurityHelper securityHelper;
  @Mock private OutboxService outboxService;

  @Spy private OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

  private InventoryService inventoryService;
  private InboxService inboxService;
  private OrderExpirationService orderExpirationService;
  private OrdersService ordersService;
  private PaymentService paymentService;
  private ShipmentsService shipmentsService;

  private User customer;
  private User staff;
  private ProductVariant variantA;
  private Product productA;
  private Inventory invA;

  @BeforeEach
  void setUp() {
    inventoryService =
        new InventoryService(
            inventoryRepository,
            stockReservationRepository,
            inventoryTransactionRepository,
            productVariantRepository,
            orderRepository,
            orderItemRepository);

    inboxService = new InboxService(inboxEventRepository);

    orderExpirationService =
        new OrderExpirationService(
            orderRepository,
            paymentRepository,
            voucherRepository,
            orderStatusHistoryRepository,
            inventoryService,
            outboxService,
            java.time.Clock.systemDefaultZone());

    CartPort cartPort = new CartPersistenceAdapter(cartRepository, cartItemRepository);
    NotificationPort notificationPort = new NotificationPersistenceAdapter(notificationRepository);

    OrderStateService orderStateService =
        new OrderStateService(orderStatusHistoryRepository, java.time.Clock.systemDefaultZone());

    ShippingFeeCalculator shippingFeeCalc = new ShippingFeeCalculator();
    ProductCatalogPort productCatalogPort =
        new ProductCatalogPersistenceAdapter(productVariantRepository, promotionVariantRepository);
    VoucherPort voucherPort = new VoucherPersistenceAdapter(voucherRepository);
    OrderPricingService orderPricingService =
        new OrderPricingService(productCatalogPort, voucherPort, shippingFeeCalc);

    ShipmentPort shipmentPort = new ShipmentPersistenceAdapter(shipmentRepository);
    InventoryPort inventoryPort = new InventoryPersistenceAdapter(inventoryService);
    PaymentPort paymentPort = new PaymentPersistenceAdapter(paymentRepository);

    OrdersQueryService ordersQueryService =
        new OrdersQueryService(
            securityHelper,
            orderRepository,
            orderItemRepository,
            orderStatusHistoryRepository,
            shipmentPort,
            null,
            orderMapper);

    OrderCreationService orderCreationService =
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
            java.time.Clock.systemDefaultZone());

    OrderFulfillmentService orderFulfillmentService =
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
            java.time.Clock.systemDefaultZone());

    OrderCancellationService orderCancellationService =
        new OrderCancellationService(
            securityHelper,
            orderRepository,
            paymentPort,
            voucherPort,
            notificationPort,
            inventoryPort,
            orderStateService,
            orderExpirationService,
            outboxService,
            java.time.Clock.systemDefaultZone());

    OrderReturnService orderReturnService =
        new OrderReturnService(
            securityHelper,
            orderRepository,
            paymentPort,
            voucherPort,
            inventoryPort,
            orderStateService,
            ordersQueryService,
            java.time.Clock.systemDefaultZone());

    ordersService =
        new OrdersService(
            orderCreationService,
            ordersQueryService,
            orderFulfillmentService,
            orderCancellationService,
            orderReturnService);

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

    shipmentsService =
        new ShipmentsService(
            securityHelper,
            shipmentRepository,
            orderRepository,
            orderItemRepository,
            orderStatusHistoryRepository,
            paymentRepository,
            inventoryRepository,
            inventoryTransactionRepository,
            notificationRepository,
            null,
            outboxService,
            inventoryService,
            orderStateService);

    customer = User.builder().userId(1L).email("user@example.com").role(Role.CUSTOMER).build();
    staff = User.builder().userId(2L).email("staff@example.com").role(Role.STAFF).build();

    productA =
        Product.builder().productId(10L).productName("iPhone 15 Pro").status("ACTIVE").build();
    variantA =
        ProductVariant.builder()
            .variantId(101L)
            .product(productA)
            .sku("IP15-128-TITAN")
            .price(BigDecimal.valueOf(25000000))
            .status("ACTIVE")
            .build();
    invA =
        Inventory.builder()
            .variant(variantA)
            .quantity(10)
            .reservedQuantity(0)
            .minThreshold(2)
            .build();

    when(inventoryRepository.reserveStockAtomic(anyLong(), anyInt(), any(LocalDateTime.class)))
        .thenReturn(1);
    lenient()
        .when(productVariantRepository.findAllById(anyCollection()))
        .thenAnswer(
            i -> {
              java.util.Collection<Long> ids = i.getArgument(0);
              return ids.stream()
                  .map(id -> productVariantRepository.findById(id).orElse(null))
                  .filter(java.util.Objects::nonNull)
                  .toList();
            });
    lenient()
        .when(
            promotionVariantRepository.findActivePromotionsForVariantIds(
                anyCollection(), any(LocalDateTime.class)))
        .thenReturn(java.util.Collections.emptyList());
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            i -> {
              Order o = i.getArgument(0);
              if (o.getOrderId() == null) {
                o.setOrderId(1001L);
              }
              return o;
            });
    when(orderRepository.findById(anyLong()))
        .thenAnswer(
            i -> {
              Long id = i.getArgument(0);
              return Optional.of(Order.builder().orderId(id).user(customer).build());
            });
  }

  // =========================================================================
  // CASE 1 & CASE 20: COD checkout không sinh QR & chặn gọi API QR
  // =========================================================================
  @Test
  @DisplayName("Case 1 & 20: Checkout COD thành công không lưu QR và chặn API tạo QR cho COD")
  void testCheckout_COD_noQr_and_rejectCreateIntent() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));
    when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(invA));
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            i -> {
              Order o = i.getArgument(0);
              o.setOrderId(1001L);
              return o;
            });

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Nguyen Van A")
            .recipientPhone("0987654321")
            .shippingAddress("123 Le Loi, Q1, HCM")
            .paymentMethod("COD")
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    CreateOrderResponse res = ordersService.createOrder(request, null);
    assertThat(res).isNotNull();
    assertThat(res.getPaymentMethod()).isEqualTo("COD");
    assertThat(res.getStatus()).isEqualTo("WAITING_CONFIRMATION");

    // Case 20: Chặn gọi createPaymentIntent với phương thức COD
    Order order =
        Order.builder()
            .orderId(1001L)
            .user(customer)
            .paymentMethod("COD")
            .paymentStatus("UNPAID")
            .totalAmount(BigDecimal.valueOf(25000000))
            .build();
    when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

    CreatePaymentIntentRequest intentReq =
        CreatePaymentIntentRequest.builder()
            .orderId(1001L)
            .paymentMethod(PaymentMethod.COD)
            .build();

    assertThatThrownBy(() -> paymentService.createPaymentIntent(intentReq))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("COD");
  }

  // =========================================================================
  // CASE 2: Checkout BANK_TRANSFER thành công, tạo QR demo có hạn dùng
  // =========================================================================
  @Test
  @DisplayName("Case 2: Checkout BANK_TRANSFER thành công, tạo QR demo và hạn dùng")
  void testCheckout_BankTransfer_withQrDemo() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    Order order =
        Order.builder()
            .orderId(2002L)
            .orderCode("DH-BT-01")
            .user(customer)
            .paymentMethod("BANK_TRANSFER")
            .paymentStatus("UNPAID")
            .status("PENDING_PAYMENT")
            .totalAmount(BigDecimal.valueOf(25000000))
            .build();

    when(orderRepository.findById(2002L)).thenReturn(Optional.of(order));
    when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(2002L))
        .thenReturn(Optional.empty());
    when(bankTransferConfig.isConfigured()).thenReturn(true);
    when(bankTransferConfig.getBankName()).thenReturn("MBBank");
    when(bankTransferConfig.getAccountNumber()).thenReturn("999999999");
    when(bankTransferConfig.getAccountName()).thenReturn("NEXTGEN TECH");
    when(bankTransferConfig.getExpireMinutes()).thenReturn(15);
    when(bankTransferConfig.buildQrContent(anyString()))
        .thenReturn("https://img.vietqr.io/demo.png");
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(
            i -> {
              Payment p = i.getArgument(0);
              p.setPaymentId(501L);
              return p;
            });

    CreatePaymentIntentRequest req =
        CreatePaymentIntentRequest.builder()
            .orderId(2002L)
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .build();

    PaymentIntentResponse res = paymentService.createPaymentIntent(req);
    assertThat(res).isNotNull();
    assertThat(res.getBankName()).isEqualTo("MBBank");
    assertThat(res.getQrContent()).isNotNull();
    assertThat(res.getExpiresAt()).isAfter(LocalDateTime.now());
  }

  // =========================================================================
  // CASE 3: Chặn checkout khi sản phẩm hoặc variant INACTIVE
  // =========================================================================
  @Test
  @DisplayName("Case 3: Chặn checkout khi biến thể INACTIVE")
  void testCheckout_rejected_whenInactive() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    ProductVariant inactiveVariant =
        ProductVariant.builder().variantId(102L).status("INACTIVE").sku("INACTIVE-SKU").build();
    when(productVariantRepository.findById(102L)).thenReturn(Optional.of(inactiveVariant));

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Test")
            .recipientPhone("0987654321")
            .shippingAddress("HCM")
            .items(List.of(OrderItemRequest.builder().variantId(102L).quantity(1).build()))
            .build();

    assertThatThrownBy(() -> ordersService.createOrder(request, null))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.PRODUCT_INACTIVE);
  }

  // =========================================================================
  // CASE 4: Gộp variantId trùng lặp khi checkout
  // =========================================================================
  @Test
  @DisplayName("Case 4: Gộp variantId trùng lặp khi checkout")
  void testCheckout_mergesDuplicateVariantIds() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));
    when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(invA));
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            i -> {
              Order o = i.getArgument(0);
              o.setOrderId(1002L);
              return o;
            });

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Test")
            .recipientPhone("0987654321")
            .shippingAddress("HCM")
            .items(
                List.of(
                    OrderItemRequest.builder().variantId(101L).quantity(2).build(),
                    OrderItemRequest.builder().variantId(101L).quantity(3).build()))
            .build();

    CreateOrderResponse res = ordersService.createOrder(request, null);
    assertThat(res.getItemsCount()).isEqualTo(1);
    verify(orderItemRepository).save(argThat(oi -> oi.getQuantity() == 5));
  }

  // =========================================================================
  // CASE 5: Chặn checkout khi số lượng vượt tồn kho khả dụng
  // =========================================================================
  @Test
  @DisplayName("Case 5: Chặn checkout khi số lượng vượt tồn kho khả dụng")
  void testCheckout_rejected_insufficientStock() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));
    Inventory lowStock =
        Inventory.builder()
            .variant(variantA)
            .quantity(2)
            .reservedQuantity(1)
            .build(); // available = 1
    when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(lowStock));
    when(inventoryRepository.reserveStockAtomic(anyLong(), anyInt(), any(LocalDateTime.class)))
        .thenReturn(0);

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Test")
            .recipientPhone("0987654321")
            .shippingAddress("HCM")
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(3).build()))
            .build();

    assertThatThrownBy(() -> ordersService.createOrder(request, null))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.ERR_STOCK_INSUFFICIENT);
  }

  // =========================================================================
  // CASE 6: Tính đúng giá Flash Sale khi checkout
  // =========================================================================
  @Test
  @DisplayName("Case 6: Áp dụng giá Flash Sale thấp hơn giá gốc khi checkout")
  void testCheckout_flashSalePrice() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA)); // price = 25tr
    when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(invA));

    PromotionVariant promo =
        PromotionVariant.builder()
            .variant(variantA)
            .promotionalPrice(BigDecimal.valueOf(20000000))
            .build();
    when(promotionVariantRepository.findActivePromotionsForVariantIds(
            anyCollection(), any(LocalDateTime.class)))
        .thenReturn(List.of(promo));

    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            i -> {
              Order o = i.getArgument(0);
              o.setOrderId(1003L);
              return o;
            });

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Test")
            .recipientPhone("0987654321")
            .shippingAddress("HCM")
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    CreateOrderResponse res = ordersService.createOrder(request, null);
    assertThat(res.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(20000000));
  }

  // =========================================================================
  // CASE 7: Áp dụng voucher hợp lệ và trừ tiền đúng
  // =========================================================================
  @Test
  @DisplayName("Case 7: Áp dụng Voucher hợp lệ trừ tiền chính xác")
  void testCheckout_validVoucher() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));
    when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(invA));

    Voucher voucher =
        Voucher.builder()
            .voucherId(10L)
            .voucherCode("GIAM100K")
            .isActive(true)
            .discountType("FIXED")
            .discountValue(BigDecimal.valueOf(100000))
            .minOrderValue(BigDecimal.valueOf(1000000))
            .usageLimit(100)
            .usedCount(10)
            .build();

    when(voucherRepository.findByVoucherCodeIgnoreCase("GIAM100K"))
        .thenReturn(Optional.of(voucher));
    when(voucherRepository.incrementUsedCountAtomic(10L)).thenReturn(1);
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            i -> {
              Order o = i.getArgument(0);
              o.setOrderId(1003L);
              return o;
            });

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Test")
            .recipientPhone("0987654321")
            .shippingAddress("HCM")
            .voucherCode("GIAM100K")
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    CreateOrderResponse res = ordersService.createOrder(request, null);
    assertThat(res.getVoucherDiscount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
    assertThat(res.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(24900000));
  }

  // =========================================================================
  // CASE 8: Chặn voucher hết hạn hoặc hết lượt dùng
  // =========================================================================
  @Test
  @DisplayName("Case 8: Chặn voucher khi đã hết lượt dùng")
  void testCheckout_rejected_voucherLimitReached() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));
    when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(invA));

    Voucher voucher =
        Voucher.builder()
            .voucherId(10L)
            .voucherCode("MAXED")
            .isActive(true)
            .usageLimit(10)
            .usedCount(10)
            .build();

    when(voucherRepository.findByVoucherCodeIgnoreCase("MAXED")).thenReturn(Optional.of(voucher));

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Test")
            .recipientPhone("0987654321")
            .shippingAddress("HCM")
            .voucherCode("MAXED")
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    assertThatThrownBy(() -> ordersService.createOrder(request, null))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.ERR_VOUCHER_LIMIT_REACHED);
  }

  // =========================================================================
  // CASE 9: Giữ chỗ tồn kho nguyên tử thành công
  // =========================================================================
  @Test
  @DisplayName("Case 9: Giữ chỗ tồn kho nguyên tử thành công")
  void testInventory_reserveStockAtomic_success() {
    Order order = Order.builder().orderId(3001L).build();
    when(orderRepository.findById(3001L)).thenReturn(Optional.of(order));
    when(stockReservationRepository.existsByOrder_OrderIdAndStatusIn(eq(3001L), anyList()))
        .thenReturn(false);
    when(inventoryRepository.reserveStockAtomic(eq(101L), eq(2), any(LocalDateTime.class)))
        .thenReturn(1);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));

    List<OrderCreatedEvent.OrderItemDto> items =
        List.of(OrderCreatedEvent.OrderItemDto.builder().variantId(101L).quantity(2).build());

    boolean ok = inventoryService.reserveStockForOrder(3001L, items, 30);
    assertThat(ok).isTrue();
    verify(stockReservationRepository)
        .save(
            argThat(
                res -> res.getStatus() == ReservationStatus.RESERVED && res.getQuantity() == 2));
  }

  // =========================================================================
  // CASE 10: Rollback giữ kho khi 1 item thiếu hàng
  // =========================================================================
  @Test
  @DisplayName("Case 10: Tự động rollback các item đã giữ khi một item sau bị thiếu hàng")
  void testInventory_rollbackWhenOneFails() {
    Order order = Order.builder().orderId(3002L).build();
    when(orderRepository.findById(3002L)).thenReturn(Optional.of(order));
    when(stockReservationRepository.existsByOrder_OrderIdAndStatusIn(eq(3002L), anyList()))
        .thenReturn(false);

    // Item 1: thành công
    when(inventoryRepository.reserveStockAtomic(eq(101L), eq(1), any(LocalDateTime.class)))
        .thenReturn(1);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));

    // Item 2: thất bại
    when(inventoryRepository.reserveStockAtomic(eq(102L), eq(5), any(LocalDateTime.class)))
        .thenReturn(0);

    List<OrderCreatedEvent.OrderItemDto> items =
        List.of(
            OrderCreatedEvent.OrderItemDto.builder().variantId(101L).quantity(1).build(),
            OrderCreatedEvent.OrderItemDto.builder().variantId(102L).quantity(5).build());

    boolean ok = inventoryService.reserveStockForOrder(3002L, items, 30);
    assertThat(ok).isFalse();
    // Kiểm tra đã gọi releaseStockAtomic cho item 101L
    verify(inventoryRepository).releaseStockAtomic(eq(101L), eq(1), any(LocalDateTime.class));
  }

  // =========================================================================
  // CASE 11: Chống giữ kho 2 lần cho cùng một Order (Idempotency)
  // =========================================================================
  @Test
  @DisplayName("Case 11: Chống giữ kho 2 lần cho cùng một Order")
  void testInventory_preventDoubleReservation() {
    when(stockReservationRepository.existsByOrder_OrderIdAndStatusIn(eq(3003L), anyList()))
        .thenReturn(true);

    boolean ok =
        inventoryService.reserveStockForOrder(
            3003L,
            List.of(OrderCreatedEvent.OrderItemDto.builder().variantId(101L).quantity(1).build()),
            30);

    assertThat(ok).isTrue();
    verify(inventoryRepository, never()).reserveStockAtomic(anyLong(), anyInt(), any());
  }

  // =========================================================================
  // CASE 12: Xuất kho vật lý MB03 thành công, trừ tồn kho và trừ reserved
  // =========================================================================
  @Test
  @DisplayName("Case 12: Xuất kho MB03 thành công, trừ tồn kho và reserved, ghi sổ cái")
  void testInventory_exportStockMB03_success() {
    Order order = Order.builder().orderId(4001L).build();
    when(orderRepository.findById(4001L)).thenReturn(Optional.of(order));
    when(inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(4001L, "EXPORT"))
        .thenReturn(false);

    StockReservation res =
        StockReservation.builder()
            .reservationId(88L)
            .order(order)
            .variant(variantA)
            .quantity(2)
            .status(ReservationStatus.RESERVED)
            .build();
    when(stockReservationRepository.findByOrder_OrderIdAndStatus(4001L, ReservationStatus.RESERVED))
        .thenReturn(List.of(res));
    when(inventoryRepository.findWithLockByVariantId(101L)).thenReturn(Optional.of(invA));

    boolean ok = inventoryService.exportStockForOrder(4001L, staff, "MB03 note");
    assertThat(ok).isTrue();
    assertThat(res.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    verify(inventoryRepository).deductStockAndReservedAtomic(eq(101L), eq(2), any());
    verify(inventoryTransactionRepository)
        .save(
            argThat(
                tx -> "EXPORT".equals(tx.getTransactionType()) && "MB03".equals(tx.getFormCode())));
  }

  // =========================================================================
  // CASE 13: Chặn xuất kho MB03 khi đơn chưa CONFIRMED
  // =========================================================================
  @Test
  @DisplayName("Case 13: Chặn xuất kho MB03 khi đơn hàng chưa được xác nhận")
  void testOrder_rejectExportMB03_whenUnconfirmed() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staff);
    Order order = Order.builder().orderId(4002L).status("WAITING_CONFIRMATION").build();
    when(orderRepository.findById(4002L)).thenReturn(Optional.of(order));

    assertThatThrownBy(
            () ->
                ordersService.packAndExportOrder(
                    4002L,
                    new com.wedbackendservice.nextgen.module.order.dto.portal.request
                        .PackExportOrderRequest()))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.ERR_ORDER_NOT_CONFIRMED);
  }

  // =========================================================================
  // CASE 14: Chặn xuất kho MB03 hai lần
  // =========================================================================
  @Test
  @DisplayName("Case 14: Chống xuất kho MB03 hai lần cho cùng một Order")
  void testInventory_preventDoubleExportMB03() {
    when(inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(4003L, "EXPORT"))
        .thenReturn(true);

    boolean ok = inventoryService.exportStockForOrder(4003L, staff, "Note");
    assertThat(ok).isTrue();
    verify(inventoryRepository, never()).deductStockAndReservedAtomic(anyLong(), anyInt(), any());
  }

  // =========================================================================
  // CASE 15: Nhập hoàn kho MB04 thành công khi giao hàng thất bại
  // =========================================================================
  @Test
  @DisplayName("Case 15: Nhập hoàn kho MB04 thành công khi giao hàng thất bại")
  void testInventory_restockMB04_success() {
    Order order = Order.builder().orderId(5001L).build();
    when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));
    when(inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(5001L, "RETURN"))
        .thenReturn(false);
    when(inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(5001L, "EXPORT"))
        .thenReturn(true);

    OrderItem oi = OrderItem.builder().variant(variantA).quantity(2).build();
    when(orderItemRepository.findByOrderId(5001L)).thenReturn(List.of(oi));
    when(inventoryRepository.findWithLockByVariantId(101L)).thenReturn(Optional.of(invA));
    when(inventoryRepository.increaseQuantityAtomic(eq(101L), eq(2), any())).thenReturn(1);

    int count = inventoryService.restockReturnOrder(5001L, staff, "Khách không nhận");
    assertThat(count).isEqualTo(1);
    verify(inventoryTransactionRepository)
        .save(
            argThat(
                tx -> "RETURN".equals(tx.getTransactionType()) && "MB04".equals(tx.getFormCode())));
  }

  // =========================================================================
  // CASE 16: Chặn nhập hoàn kho MB04 hai lần
  // =========================================================================
  @Test
  @DisplayName("Case 16: Chống nhập hoàn kho MB04 hai lần")
  void testInventory_preventDoubleRestockMB04() {
    when(inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(5002L, "RETURN"))
        .thenReturn(true);

    int count = inventoryService.restockReturnOrder(5002L, staff, "Note");
    assertThat(count).isEqualTo(0);
    verify(inventoryRepository, never()).increaseQuantityAtomic(anyLong(), anyInt(), any());
  }

  @Test
  @DisplayName("Case 16b: Kiểm định MB04 hư hại chuyển kho kiểm định/thanh lý và không cộng tồn khả dụng")
  void testInventory_damagedReturnMB04_doesNotIncreaseStock() {
    Order order = Order.builder().orderId(5003L).orderCode("DH5003").build();
    when(orderRepository.findById(5003L)).thenReturn(Optional.of(order));
    when(inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(5003L, "RETURN"))
        .thenReturn(false);
    when(inventoryTransactionRepository.existsByOrder_OrderIdAndTransactionType(5003L, "EXPORT"))
        .thenReturn(true);

    OrderItem oi = OrderItem.builder().variant(variantA).quantity(2).build();
    when(orderItemRepository.findByOrderId(5003L)).thenReturn(List.of(oi));
    when(inventoryRepository.findWithLockByVariantId(101L)).thenReturn(Optional.of(invA));

    int count = inventoryService.restockReturnOrder(5003L, staff, "Rách seal, móp hộp", false);
    assertThat(count).isEqualTo(0);
    verify(inventoryRepository, never()).increaseQuantityAtomic(anyLong(), anyInt(), any());
    verify(inventoryTransactionRepository)
        .save(
            argThat(
                tx ->
                    "RETURN".equals(tx.getTransactionType())
                        && "MB04".equals(tx.getFormCode())
                        && tx.getQuantity() == 0
                        && tx.getReason() != null
                        && tx.getReason().contains("[KHO KIỂM ĐỊNH / THANH LÝ]")));
  }

  // =========================================================================
  // CASE 17: Giả lập thanh toán BANK_TRANSFER thành công
  // =========================================================================
  @Test
  @DisplayName(
      "Case 17: Giả lập thanh toán BANK_TRANSFER thành công chuyển đơn sang WAITING_CONFIRMATION")
  void testPayment_simulateSuccess() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    Order order =
        Order.builder()
            .orderId(6001L)
            .orderCode("DH6001")
            .user(customer)
            .status("PENDING_PAYMENT")
            .paymentStatus("UNPAID")
            .build();
    Payment payment =
        Payment.builder()
            .paymentId(701L)
            .order(order)
            .paymentMethod("BANK_TRANSFER")
            .amount(BigDecimal.valueOf(25000000))
            .transactionRef("TXN-701")
            .status(PaymentStatus.PENDING.name())
            .build();

    when(paymentRepository.findById(701L)).thenReturn(Optional.of(payment));

    SimulateCallbackResponse res = paymentService.simulateSuccess(701L);
    assertThat(res.getStatus()).isEqualTo("SUCCESS");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.WAITING_CONFIRMATION.name());
    assertThat(order.getPaymentStatus()).isEqualTo("PAID");
  }

  // =========================================================================
  // CASE 18: Giả lập thanh toán BANK_TRANSFER thất bại, hủy đơn và nhả kho
  // =========================================================================
  @Test
  @DisplayName("Case 18: Giả lập thanh toán thất bại, hủy đơn và nhả tồn kho")
  void testPayment_simulateFailure() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    Order order =
        Order.builder()
            .orderId(6002L)
            .orderCode("DH6002")
            .user(customer)
            .status("PENDING_PAYMENT")
            .paymentStatus("UNPAID")
            .build();
    Payment payment =
        Payment.builder()
            .paymentId(702L)
            .order(order)
            .paymentMethod("BANK_TRANSFER")
            .status(PaymentStatus.PENDING.name())
            .build();

    when(paymentRepository.findById(702L)).thenReturn(Optional.of(payment));

    StockReservation res =
        StockReservation.builder()
            .variant(variantA)
            .quantity(1)
            .status(ReservationStatus.RESERVED)
            .build();
    when(stockReservationRepository.findByOrder_OrderIdAndStatus(6002L, ReservationStatus.RESERVED))
        .thenReturn(List.of(res));

    SimulateCallbackResponse callbackRes = paymentService.simulateFailure(702L, "Khách hết tiền");
    assertThat(callbackRes.getStatus()).isEqualTo("FAILED");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED.name());
    verify(inventoryRepository).releaseStockAtomic(eq(101L), eq(1), any());
  }

  // =========================================================================
  // CASE 19: Chặn thanh toán cho đơn hàng đã CANCELLED hoặc EXPIRED
  // =========================================================================
  @Test
  @DisplayName("Case 19: Chặn thanh toán cho đơn hàng đã bị CANCELLED")
  void testPayment_rejected_whenOrderCancelled() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    Order order = Order.builder().orderId(6003L).user(customer).status("CANCELLED").build();
    Payment payment =
        Payment.builder()
            .paymentId(703L)
            .order(order)
            .paymentMethod("BANK_TRANSFER")
            .status(PaymentStatus.PENDING.name())
            .build();

    when(paymentRepository.findById(703L)).thenReturn(Optional.of(payment));

    assertThatThrownBy(() -> paymentService.simulateSuccess(703L))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("hủy hoặc hết hạn");
  }

  // =========================================================================
  // CASE 21: Chuyển trạng thái Order không hợp lệ bị từ chối
  // =========================================================================
  @Test
  @DisplayName("Case 21: Chặn xác nhận đơn hàng khi đơn chưa ở trạng thái hợp lệ")
  void testOrder_invalidStateTransition_rejected() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staff);
    Order order = Order.builder().orderId(8001L).status("COMPLETED").build();
    when(orderRepository.findById(8001L)).thenReturn(Optional.of(order));

    com.wedbackendservice.nextgen.module.order.dto.portal.request.ConfirmOrderRequest req =
        new com.wedbackendservice.nextgen.module.order.dto.portal.request.ConfirmOrderRequest();
    req.setCarrier("GHN");

    assertThatThrownBy(() -> ordersService.confirmOrder(8001L, req))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.ERR_ORDER_INVALID_STATE);
  }

  // =========================================================================
  // CASE 22: Scheduler quét và hủy đơn BANK_TRANSFER hết hạn, nhả kho đúng 1 lần
  // =========================================================================
  @Test
  @DisplayName("Case 22: Quét đơn hết hạn, nhả tồn kho và chuyển payment sang EXPIRED")
  void testScheduler_cancelExpiredOrders() {
    Order expOrder = Order.builder().orderId(9001L).status("PENDING_PAYMENT").build();
    when(orderRepository.findExpiredPendingOrders(any(LocalDateTime.class)))
        .thenReturn(List.of(expOrder));
    when(orderRepository.findById(9001L)).thenReturn(Optional.of(expOrder));
    when(orderItemRepository.findByOrderIdIn(anyList())).thenReturn(List.of());

    Payment pendingPay =
        Payment.builder()
            .paymentId(991L)
            .order(expOrder)
            .status(PaymentStatus.PENDING.name())
            .build();
    when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(9001L))
        .thenReturn(Optional.of(pendingPay));

    int cancelled = ordersService.cancelExpiredOrders();
    assertThat(cancelled).isEqualTo(1);
    assertThat(expOrder.getStatus()).isEqualTo("CANCELLED");
    assertThat(pendingPay.getStatus()).isEqualTo(PaymentStatus.EXPIRED.name());
  }

  // =========================================================================
  // CASE 23: Giao hàng COD thành công nhưng chưa thu tiền: Order DELIVERED, Payment UNPAID
  // =========================================================================
  @Test
  @DisplayName(
      "Case 23: Vận đơn DELIVERED nhưng chưa thu tiền COD -> Order DELIVERED, Payment UNPAID")
  void testShipment_delivered_withoutCodCollection() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staff);
    Order order =
        Order.builder()
            .orderId(1101L)
            .orderCode("DH1101")
            .paymentMethod("COD")
            .paymentStatus("UNPAID")
            .status("SHIPPING")
            .totalAmount(BigDecimal.valueOf(100000))
            .build();
    Shipment shipment =
        Shipment.builder()
            .shipmentId(2201L)
            .trackingNumber("GHN-1101")
            .order(order)
            .shippingStatus("IN_TRANSIT")
            .build();

    when(shipmentRepository.findById(2201L)).thenReturn(Optional.of(shipment));

    ShipmentStatusUpdateRequest req =
        ShipmentStatusUpdateRequest.builder().status("DELIVERED").codCollected(false).build();

    shipmentsService.updateShipmentStatus(2201L, req);
    assertThat(order.getStatus()).isEqualTo("DELIVERED");
    assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
  }

  // =========================================================================
  // CASE 24: Xác nhận thu tiền COD: Payment SUCCESS, Order COMPLETED
  // =========================================================================
  @Test
  @DisplayName("Case 24: Xác nhận đã thu tiền COD -> Order COMPLETED, Payment SUCCESS")
  void testShipment_delivered_withCodCollection() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staff);
    Order order =
        Order.builder()
            .orderId(1102L)
            .orderCode("DH1102")
            .paymentMethod("COD")
            .paymentStatus("UNPAID")
            .status("SHIPPING")
            .totalAmount(BigDecimal.valueOf(100000))
            .build();
    Shipment shipment =
        Shipment.builder()
            .shipmentId(2202L)
            .trackingNumber("GHN-1102")
            .order(order)
            .shippingStatus("IN_TRANSIT")
            .build();
    Payment payment =
        Payment.builder().paymentId(3301L).order(order).status(PaymentStatus.UNPAID.name()).build();

    when(shipmentRepository.findById(2202L)).thenReturn(Optional.of(shipment));
    when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(1102L))
        .thenReturn(Optional.of(payment));

    ShipmentStatusUpdateRequest req =
        ShipmentStatusUpdateRequest.builder().status("DELIVERED").codCollected(true).build();

    shipmentsService.updateShipmentStatus(2202L, req);
    assertThat(order.getStatus()).isEqualTo("COMPLETED");
    assertThat(order.getPaymentStatus()).isEqualTo("PAID");
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS.name());
  }

  // =========================================================================
  // CASE 25: Idempotent Consumer: Gửi trùng event Kafka chỉ xử lý đúng một lần
  // =========================================================================
  @Test
  @DisplayName("Case 25: InboxService kiểm tra chống xử lý trùng event (Idempotency)")
  void testInbox_idempotentConsumer() {
    when(inboxEventRepository.existsByEventIdAndConsumerGroup("evt-12345", "nextgen-order-saga"))
        .thenReturn(true);

    boolean alreadyProcessed = inboxService.isAlreadyProcessed("evt-12345", "nextgen-order-saga");
    assertThat(alreadyProcessed).isTrue();

    // Kiểm tra markAsProcessed lưu vào inbox
    inboxService.markAsProcessed("evt-99999", "nextgen-order-saga", "OrderCreated", "DH-999");
    verify(inboxEventRepository)
        .save(
            argThat(
                evt ->
                    "evt-99999".equals(evt.getEventId())
                        && "nextgen-order-saga".equals(evt.getConsumerGroup())));
  }

  // =========================================================================

  // =========================================================================

  // =========================================================================
  // CASE 26: Áp dụng Voucher PERCENT có trần maxDiscount
  // =========================================================================
  @Test
  @DisplayName("Case 26: Áp dụng Voucher PERCENT có trần maxDiscount")
  void testCheckout_voucherPercent_withMaxDiscount_capped() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));
    when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(invA));

    // Giảm 10% của 25tr là 2.5tr, nhưng maxDiscountAmount = 500k -> chỉ giảm 500k
    Voucher voucher =
        Voucher.builder()
            .voucherId(20L)
            .voucherCode("SALE10PERCENT")
            .isActive(true)
            .discountType("PERCENT")
            .discountValue(BigDecimal.valueOf(10))
            .maxDiscountAmount(BigDecimal.valueOf(500000))
            .minOrderValue(BigDecimal.valueOf(1000000))
            .usageLimit(100)
            .usedCount(5)
            .build();

    when(voucherRepository.findByVoucherCodeIgnoreCase("SALE10PERCENT"))
        .thenReturn(Optional.of(voucher));
    when(voucherRepository.incrementUsedCountAtomic(20L)).thenReturn(1);
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            i -> {
              Order o = i.getArgument(0);
              o.setOrderId(1004L);
              return o;
            });

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Nguyen Van A")
            .recipientPhone("0987654321")
            .shippingAddress("Ha Noi")
            .voucherCode("SALE10PERCENT")
            .paymentMethod("COD")
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    CreateOrderResponse res = ordersService.createOrder(request, null);
    assertThat(res.getVoucherDiscount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
    assertThat(res.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(24500000));
  }

  // =========================================================================
  // CASE 27: Khi có nhiều chương trình khuyến mãi cùng áp dụng, chọn giá thấp nhất
  // =========================================================================
  @Test
  @DisplayName("Case 27: Khi có nhiều chương trình khuyến mãi cùng áp dụng, chọn giá thấp nhất")
  void testCheckout_multiplePromotions_picksLowestPrice() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));
    when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(invA));

    PromotionVariant promo1 =
        PromotionVariant.builder()
            .variant(variantA)
            .promotionalPrice(BigDecimal.valueOf(23000000))
            .build();
    PromotionVariant promo2 =
        PromotionVariant.builder()
            .variant(variantA)
            .promotionalPrice(BigDecimal.valueOf(21000000))
            .build();
    PromotionVariant promo3 =
        PromotionVariant.builder()
            .variant(variantA)
            .promotionalPrice(BigDecimal.valueOf(24000000))
            .build();

    when(promotionVariantRepository.findActivePromotionsForVariantIds(
            anyCollection(), any(LocalDateTime.class)))
        .thenReturn(List.of(promo1, promo2, promo3));

    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            i -> {
              Order o = i.getArgument(0);
              o.setOrderId(1005L);
              return o;
            });

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Nguyen Van A")
            .recipientPhone("0987654321")
            .shippingAddress("Ha Noi")
            .paymentMethod("COD")
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    CreateOrderResponse res = ordersService.createOrder(request, null);
    assertThat(res.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(21000000));
    assertThat(res.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(21000000));
  }

  // =========================================================================
  // CASE 28: Idempotency key đang PROCESSING -> ném ERR_ORDER_PROCESSING (409)
  // =========================================================================
  @Test
  @DisplayName("Case 28: Idempotency key đang PROCESSING -> ném ERR_ORDER_PROCESSING (409)")
  void testCheckout_idempotency_processing_throwsConflict() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(tokenRedisService.setIfAbsent(
            contains("idempotency:order:1:key-123"), eq("PROCESSING"), any(Duration.class)))
        .thenReturn(false);
    when(tokenRedisService.getValue(contains("idempotency:order:1:key-123")))
        .thenReturn("PROCESSING");

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Nguyen Van A")
            .recipientPhone("0987654321")
            .shippingAddress("Ha Noi")
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    assertThatThrownBy(() -> ordersService.createOrder(request, "key-123"))
        .isInstanceOf(AppException.class)
        .satisfies(
            e ->
                assertThat(((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ERR_ORDER_PROCESSING));
  }

  // =========================================================================
  // CASE 29: Idempotency key đã hoàn tất -> trả về đơn hàng cũ
  // =========================================================================
  @Test
  @DisplayName("Case 29: Idempotency key đã hoàn tất -> trả về đơn hàng cũ")
  void testCheckout_idempotency_completed_returnsExistingOrder() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(tokenRedisService.setIfAbsent(
            contains("idempotency:order:1:key-456"), eq("PROCESSING"), any(Duration.class)))
        .thenReturn(false);
    when(tokenRedisService.getValue(contains("idempotency:order:1:key-456")))
        .thenReturn("DH-EXISTING-999");

    Order existingOrder =
        Order.builder()
            .orderId(999L)
            .orderCode("DH-EXISTING-999")
            .user(customer)
            .status("PENDING_PAYMENT")
            .paymentMethod("BANK_TRANSFER")
            .totalAmount(BigDecimal.valueOf(25000000))
            .build();
    when(orderRepository.findByOrderCode("DH-EXISTING-999")).thenReturn(Optional.of(existingOrder));
    when(orderItemRepository.findByOrderId(999L))
        .thenReturn(
            List.of(
                OrderItem.builder()
                    .orderItemId(1L)
                    .order(existingOrder)
                    .price(BigDecimal.valueOf(25000000))
                    .quantity(1)
                    .build()));

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Nguyen Van A")
            .recipientPhone("0987654321")
            .shippingAddress("Ha Noi")
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    CreateOrderResponse res = ordersService.createOrder(request, "key-456");
    assertThat(res.getOrderCode()).isEqualTo("DH-EXISTING-999");
    assertThat(res.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(25000000));
    verify(orderRepository, never()).save(any(Order.class));
  }

  // =========================================================================
  // CASE 30: expectedTotalAmount không khớp với tổng tiền server tính -> ném INVALID_REQUEST
  // =========================================================================
  @Test
  @DisplayName(
      "Case 30: expectedTotalAmount không khớp với tổng tiền server tính -> ném INVALID_REQUEST")
  void testCheckout_expectedTotalAmount_mismatch_throwsException() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variantA));
    when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(invA));

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Nguyen Van A")
            .recipientPhone("0987654321")
            .shippingAddress("Ha Noi")
            .paymentMethod("COD")
            .expectedTotalAmount(BigDecimal.valueOf(20000000)) // Client gửi 20tr nhưng thực tế 25tr
            .items(List.of(OrderItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    assertThatThrownBy(() -> ordersService.createOrder(request, null))
        .isInstanceOf(AppException.class)
        .satisfies(
            e ->
                assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
  }

  // =========================================================================
  // CASE 31: Hủy đơn hàng đã PAID -> chuyển Order và Payment sang REFUND_PENDING, hoàn kho và
  // voucher
  // =========================================================================
  @Test
  @DisplayName(
      "Case 31: Hủy đơn hàng đã PAID -> chuyển Order và Payment sang REFUND_PENDING, hoàn kho và voucher")
  void testCancelOrder_paidOrder_transitionsToRefundPending_andReleasesStockAndVoucher() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);

    Voucher voucher = Voucher.builder().voucherId(55L).voucherCode("GIAM50K").usedCount(2).build();
    Order order =
        Order.builder()
            .orderId(888L)
            .orderCode("DH888")
            .user(customer)
            .status("PENDING_PAYMENT")
            .paymentStatus("PAID")
            .totalAmount(BigDecimal.valueOf(25000000))
            .finalAmount(BigDecimal.valueOf(25000000))
            .voucher(voucher)
            .build();

    Payment payment =
        Payment.builder()
            .paymentId(777L)
            .order(order)
            .status(PaymentStatus.SUCCESS.name())
            .amount(BigDecimal.valueOf(25000000))
            .build();

    StockReservation reservation =
        StockReservation.builder()
            .reservationId(10L)
            .order(order)
            .variant(variantA)
            .quantity(1)
            .status(ReservationStatus.RESERVED)
            .build();

    when(orderRepository.findByOrderCode("DH888")).thenReturn(Optional.of(order));
    when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(888L))
        .thenReturn(Optional.of(payment));
    when(stockReservationRepository.findByOrder_OrderIdAndStatus(
            eq(888L), eq(ReservationStatus.RESERVED)))
        .thenReturn(List.of(reservation));
    when(voucherRepository.decrementUsedCountAtomic(55L)).thenReturn(1);

    CancelOrderRequest req =
        CancelOrderRequest.builder().reason("Khách đổi ý muốn mua màu khác").build();
    CancelOrderResponse res = ordersService.cancelOrder("DH888", req);

    assertThat(res.getStatus()).isEqualTo("CANCELLED");
    assertThat(res.getReleasedHoldStock()).isTrue();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED.name());
    assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING.name());
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING.name());

    verify(inventoryRepository).releaseStockAtomic(eq(101L), eq(1), any(LocalDateTime.class));
    verify(voucherRepository).decrementUsedCountAtomic(55L);
    verify(outboxService)
        .saveEvent(
            eq("ORDER"), eq("DH888"), eq("RefundRequested"), eq("order-cancelled-topic"), anyMap());
  }

  // =========================================================================
  // CASE 32: Chặn hủy đơn hàng khi đã được xác nhận (CONFIRMED)
  // =========================================================================
  @Test
  @DisplayName("Case 32: Chặn hủy đơn hàng khi đã được xác nhận (CONFIRMED)")
  void testCancelOrder_rejected_whenStatusConfirmed() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);

    Order order =
        Order.builder()
            .orderId(889L)
            .orderCode("DH889")
            .user(customer)
            .status("CONFIRMED")
            .paymentStatus("PAID")
            .build();

    when(orderRepository.findByOrderCode("DH889")).thenReturn(Optional.of(order));

    CancelOrderRequest req = CancelOrderRequest.builder().reason("Muốn hủy").build();

    assertThatThrownBy(() -> ordersService.cancelOrder("DH889", req))
        .isInstanceOf(AppException.class)
        .satisfies(
            e ->
                assertThat(((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ERR_ORDER_CANNOT_CANCEL));
  }

  // =========================================================================
  // CASE 33: Hủy đơn hàng đã CUSTOMER_NOTIFIED được coi như đã trả tiền -> REFUND_PENDING
  // =========================================================================
  @Test
  @DisplayName("Case 33: Hủy đơn hàng CUSTOMER_NOTIFIED chuyển sang REFUND_PENDING")
  void testCancelOrder_customerNotified_marksRefundPending() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);

    Order order =
        Order.builder()
            .orderId(890L)
            .orderCode("DH890")
            .user(customer)
            .status("PENDING_PAYMENT")
            .paymentStatus("CUSTOMER_NOTIFIED")
            .paymentMethod("BANK_TRANSFER")
            .finalAmount(BigDecimal.valueOf(1500000))
            .build();

    StockReservation resv =
        StockReservation.builder()
            .reservationId(501L)
            .order(order)
            .variant(variantA)
            .quantity(1)
            .status(ReservationStatus.RESERVED)
            .build();

    Payment payment =
        Payment.builder().paymentId(701L).order(order).status("CUSTOMER_NOTIFIED").build();

    when(orderRepository.findByOrderCode("DH890")).thenReturn(Optional.of(order));
    when(orderRepository.findByOrderCodeForUpdate("DH890")).thenReturn(Optional.of(order));
    when(stockReservationRepository.findByOrder_OrderIdAndStatus(890L, ReservationStatus.RESERVED))
        .thenReturn(List.of(resv));
    when(inventoryRepository.releaseStockAtomic(eq(101L), eq(1), any(LocalDateTime.class)))
        .thenReturn(1);
    when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(890L))
        .thenReturn(Optional.of(payment));

    CancelOrderRequest req =
        CancelOrderRequest.builder().reason("Khách chuyển nhầm muốn hoàn tiền").build();
    CancelOrderResponse res = ordersService.cancelOrder("DH890", req);

    assertThat(res.getStatus()).isEqualTo("CANCELLED");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED.name());
    assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING.name());
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING.name());
    verify(outboxService)
        .saveEvent(
            eq("ORDER"), eq("DH890"), eq("RefundRequested"), eq("order-cancelled-topic"), anyMap());
  }

  // =========================================================================
  // CASE 34: Hủy đơn hàng với lý do null không bị NullPointerException
  // =========================================================================
  @Test
  @DisplayName("Case 34: Hủy đơn hàng với reason null an toàn tuyệt đối")
  void testCancelOrder_nullReason_handledSafely() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);

    Order order =
        Order.builder()
            .orderId(891L)
            .orderCode("DH891")
            .user(customer)
            .status("WAITING_CONFIRMATION")
            .paymentStatus("UNPAID")
            .paymentMethod("COD")
            .finalAmount(BigDecimal.valueOf(500000))
            .build();

    when(orderRepository.findByOrderCode("DH891")).thenReturn(Optional.of(order));
    when(orderRepository.findByOrderCodeForUpdate("DH891")).thenReturn(Optional.of(order));
    when(stockReservationRepository.findByOrder_OrderIdAndStatus(891L, ReservationStatus.RESERVED))
        .thenReturn(Collections.emptyList());

    CancelOrderRequest req = CancelOrderRequest.builder().reason(null).build();
    CancelOrderResponse res = ordersService.cancelOrder("DH891", req);

    assertThat(res.getStatus()).isEqualTo("CANCELLED");
    assertThat(res.getCancelReason()).isNotBlank();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED.name());
  }

  // =========================================================================
  // CASE 35: Chặn đặt hàng khi biến thể sản phẩm chưa có giá hoặc giá <= 0
  // =========================================================================
  @Test
  @DisplayName("Case 35: Chặn đặt hàng khi biến thể sản phẩm chưa có giá hợp lệ")
  void testCheckout_variantWithNullOrZeroPrice_throwsInvalidRequest() {
    when(securityHelper.getCurrentUser()).thenReturn(customer);

    ProductVariant invalidPriceVariant =
        ProductVariant.builder()
            .variantId(109L)
            .status("ACTIVE")
            .sku("INVALID-PRICE-SKU")
            .price(null) // Không có giá
            .build();

    when(productVariantRepository.findAllById(anyList())).thenReturn(List.of(invalidPriceVariant));

    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .recipientName("Nguyen Van A")
            .recipientPhone("0987654321")
            .shippingAddress("123 Duong ABC, Quan 1, TP.HCM")
            .paymentMethod("COD")
            .items(List.of(OrderItemRequest.builder().variantId(109L).quantity(1).build()))
            .build();

    assertThatThrownBy(() -> ordersService.createOrder(request, null))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> {
              AppException ae = (AppException) e;
              assertThat(ae.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
              assertThat(ae.getMessage()).contains("chưa có giá hợp lệ");
            });
  }
}
