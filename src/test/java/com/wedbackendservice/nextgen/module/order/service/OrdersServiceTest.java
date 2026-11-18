package com.wedbackendservice.nextgen.module.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.module.cart.repository.CartItemRepository;
import com.wedbackendservice.nextgen.module.cart.repository.CartRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryTransactionRepository;
import com.wedbackendservice.nextgen.module.inventory.service.InventoryService;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.InventoryPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.NotificationPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.PaymentPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.adapter.out.persistence.ShipmentPersistenceAdapter;
import com.wedbackendservice.nextgen.module.order.dto.portal.request.PackExportOrderRequest;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.mapper.OrderMapper;
import com.wedbackendservice.nextgen.module.order.port.out.inventory.InventoryPort;
import com.wedbackendservice.nextgen.module.order.port.out.notification.NotificationPort;
import com.wedbackendservice.nextgen.module.order.port.out.payment.PaymentPort;
import com.wedbackendservice.nextgen.module.order.port.out.shipment.ShipmentPort;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.order.repository.ReturnRequestRepository;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import com.wedbackendservice.nextgen.module.product.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.module.shipment.repository.ShipmentRepository;
import com.wedbackendservice.nextgen.module.shipment.service.ShippingFeeCalculator;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.voucher.repository.PromotionVariantRepository;
import com.wedbackendservice.nextgen.module.voucher.repository.VoucherRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {

  @Mock private SecurityHelper securityHelper;
  @Mock private OrderRepository orderRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
  @Mock private InventoryRepository inventoryRepository;
  @Mock private InventoryTransactionRepository inventoryTransactionRepository;
  @Mock private ProductVariantRepository productVariantRepository;
  @Mock private VoucherRepository voucherRepository;
  @Mock private PromotionVariantRepository promotionVariantRepository;
  @Mock private ShipmentRepository shipmentRepository;
  @Mock private ReturnRequestRepository returnRequestRepository;
  @Mock private CartRepository cartRepository;
  @Mock private CartItemRepository cartItemRepository;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private TokenRedisService tokenRedisService;
  @Mock private PaymentRepository paymentRepository;
  @Mock private OutboxService outboxService;
  @Mock private InventoryService inventoryService;
  @Mock private ShippingFeeCalculator shippingFeeCalculator;
  @Mock private OrderExpirationService orderExpirationService;
  @Mock private OrderPostCommitService orderPostCommitService;
  @Spy private java.time.Clock clock = java.time.Clock.systemDefaultZone();
  @Spy private OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

  @Mock private NotificationRepository notificationRepository;
  @Mock private OrderStateService orderStateService;
  @Mock private OrdersQueryService ordersQueryService;

  private OrderFulfillmentService orderFulfillmentService;
  private OrdersService ordersService;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    ShipmentPort shipmentPort = new ShipmentPersistenceAdapter(shipmentRepository);
    PaymentPort paymentPort = new PaymentPersistenceAdapter(paymentRepository);
    NotificationPort notificationPort = new NotificationPersistenceAdapter(notificationRepository);
    InventoryPort inventoryPort = new InventoryPersistenceAdapter(inventoryService);

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
            clock);
    ordersService =
        new OrdersService(null, ordersQueryService, orderFulfillmentService, null, null);
  }

  @Test
  void packAndExportOrder_throwsWhenAtomicStockDeductionReturnsZero() {
    User staff = User.builder().userId(2L).fullName("Staff").role(Role.STAFF).build();
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staff);

    Order order = Order.builder().orderId(10L).orderCode("DH10").status("CONFIRMED").build();
    when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

    when(inventoryService.exportStockForOrderWithDetails(eq(10L), eq(staff), any()))
        .thenReturn(java.util.List.of());

    assertThatThrownBy(() -> ordersService.packAndExportOrder(10L, new PackExportOrderRequest()))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.ERR_STOCK_INSUFFICIENT);
  }

  @Test
  void customerNotifyPayment_rejectsNonOwnerCustomer() {
    User attacker = User.builder().userId(99L).fullName("Other").role(Role.CUSTOMER).build();
    User owner = User.builder().userId(10L).fullName("Owner").role(Role.CUSTOMER).build();
    Order order =
        Order.builder()
            .orderId(500L)
            .orderCode("DH500")
            .user(owner)
            .paymentStatus("UNPAID")
            .build();

    when(securityHelper.getCurrentUser()).thenReturn(attacker);
    when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> ordersService.customerNotifyPayment(500L, "Đã chuyển khoản"))
        .isInstanceOf(AppException.class)
        .extracting(ex -> ((AppException) ex).getErrorCode())
        .isEqualTo(ErrorCode.UNAUTHORIZED);
  }
}
