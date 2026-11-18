package com.wedbackendservice.nextgen.module.shipment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryTransactionRepository;
import com.wedbackendservice.nextgen.module.inventory.service.InventoryService;
import com.wedbackendservice.nextgen.module.notification.entity.Notification;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderItem;
import com.wedbackendservice.nextgen.module.order.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.payment.entity.Payment;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.shipment.dto.client.response.ShipmentTrackResponse;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.request.ShipmentFailDeliveryRequest;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.request.ShipmentStatusUpdateRequest;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.response.ShipmentFailDeliveryResponse;
import com.wedbackendservice.nextgen.module.shipment.dto.portal.response.ShipmentStatusSyncResponse;
import com.wedbackendservice.nextgen.module.shipment.entity.Shipment;
import com.wedbackendservice.nextgen.module.shipment.mapper.ShipmentMapper;
import com.wedbackendservice.nextgen.module.shipment.repository.ShipmentRepository;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentsServiceTest {

  @Mock private SecurityHelper securityHelper;

  @Mock private ShipmentRepository shipmentRepository;

  @Mock private OrderRepository orderRepository;

  @Mock private OrderItemRepository orderItemRepository;

  @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;

  @Mock private PaymentRepository paymentRepository;

  @Mock private InventoryRepository inventoryRepository;

  @Mock private InventoryTransactionRepository inventoryTransactionRepository;

  @Mock private NotificationRepository notificationRepository;

  @Mock private InventoryService inventoryService;

  @Mock private OutboxService outboxService;

  @Spy private ShipmentMapper shipmentMapper = Mappers.getMapper(ShipmentMapper.class);

  @InjectMocks private ShipmentsService shipmentsService;

  private User staffUser;
  private User customer;
  private Order order;
  private Shipment shipment;
  private ProductVariant variant;
  private OrderItem orderItem;

  @BeforeEach
  void setUp() {
    staffUser = User.builder().userId(1L).fullName("Staff Nguyen").role(Role.STAFF).build();

    customer = User.builder().userId(2L).fullName("Nguyen Van A").phone("0912345678").build();

    order =
        Order.builder()
            .orderId(100L)
            .orderCode("ORD-100")
            .user(customer)
            .recipientName("Nguyen Van A")
            .recipientPhone("0912345678")
            .shippingAddress("123 Nguyen Trai, Q5, TPHCM")
            .status("SHIPPING")
            .paymentStatus("PENDING")
            .totalAmount(BigDecimal.valueOf(25000000))
            .build();

    shipment =
        Shipment.builder()
            .shipmentId(10L)
            .order(order)
            .trackingNumber("VN123456789")
            .carrierName("GHTK")
            .shippingStatus("IN_TRANSIT")
            .recipientName("Nguyen Van A")
            .recipientPhone("0912345678")
            .shippingAddress("123 Nguyen Trai, Q5, TPHCM")
            .build();

    variant = ProductVariant.builder().variantId(50L).sku("SKU-50").build();

    orderItem =
        OrderItem.builder().orderItemId(500L).order(order).variant(variant).quantity(2).build();
  }

  @Test
  void trackShipment_Success_MasksPII() {
    when(shipmentRepository.findByTrackingCodeOrOrderCode("VN123456789"))
        .thenReturn(Optional.of(shipment));
    when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(orderItem));

    ShipmentTrackResponse response = shipmentsService.trackShipment("VN123456789");

    assertThat(response).isNotNull();
    assertThat(response.getTrackingCode()).isEqualTo("VN123456789");
    assertThat(response.getCarrier()).isEqualTo("GHTK");
    assertThat(response.getStatus()).isEqualTo("IN_TRANSIT");
    // Kiểm tra che PII
    assertThat(response.getRecipientPhone()).isEqualTo("091*****78");
    assertThat(response.getRecipientName()).isEqualTo("Nguyen V*** A***");
  }

  @Test
  void trackShipment_NotFound_ThrowsException() {
    when(shipmentRepository.findByTrackingCodeOrOrderCode("NOT_FOUND"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> shipmentsService.trackShipment("NOT_FOUND"))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.SHIPMENT_NOT_FOUND);
  }

  @Test
  void updateShipmentStatus_Delivered_CompletesOrderAndPayment() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

    Payment payment = Payment.builder().paymentId(1L).order(order).status("PENDING").build();
    when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(100L))
        .thenReturn(Optional.of(payment));

    ShipmentStatusUpdateRequest req =
        ShipmentStatusUpdateRequest.builder()
            .status("DELIVERED")
            .note("Khách đã nhận hàng")
            .build();

    ShipmentStatusSyncResponse res = shipmentsService.updateShipmentStatus(10L, req);

    assertThat(res).isNotNull();
    assertThat(res.getShipmentStatus()).isEqualTo("DELIVERED");
    assertThat(res.getSynchronizedOrderStatus()).isEqualTo("COMPLETED");
    assertThat(res.getIsPaymentCompleted()).isTrue();

    assertThat(order.getStatus()).isEqualTo("COMPLETED");
    assertThat(payment.getStatus()).isEqualTo("SUCCESS");
    verify(shipmentRepository).save(shipment);
    verify(orderRepository, atLeastOnce()).save(order);
    verify(paymentRepository).save(payment);
    verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
  }

  @Test
  void failDelivery_Success_RestocksInventory() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
    when(inventoryService.restockReturnOrder(eq(100L), eq(staffUser), any())).thenReturn(1);

    ShipmentFailDeliveryRequest req =
        ShipmentFailDeliveryRequest.builder()
            .failReason("Khách không nghe máy sau 3 lần gọi")
            .build();

    ShipmentFailDeliveryResponse res = shipmentsService.failDelivery(10L, req);

    assertThat(res).isNotNull();
    assertThat(res.getShipmentStatus()).isEqualTo("FAILED_DELIVERY");
    assertThat(res.getOrderStatus()).isEqualTo("RETURNING");
    assertThat(res.getInventoryRestocked()).isTrue();
    assertThat(res.getRestockedItemsCount()).isEqualTo(1);

    assertThat(order.getStatus()).isEqualTo("RETURNING");
    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  void failDelivery_AlreadyDelivered_ThrowsInvalidRequest() {
    shipment.setShippingStatus("DELIVERED");
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

    ShipmentFailDeliveryRequest req =
        ShipmentFailDeliveryRequest.builder().failReason("Hàng hỏng").build();

    assertThatThrownBy(() -> shipmentsService.failDelivery(10L, req))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_REQUEST);

    verify(inventoryRepository, never()).increaseQuantityAtomic(anyLong(), anyInt(), any());
  }

  @Test
  void updateShipmentStatus_SkipStep_ThrowsException() {
    shipment.setShippingStatus("READY_TO_PICK");
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

    // Thử nhảy cóc từ READY_TO_PICK sang IN_TRANSIT
    ShipmentStatusUpdateRequest reqInTransit =
        ShipmentStatusUpdateRequest.builder().status("IN_TRANSIT").build();

    assertThatThrownBy(() -> shipmentsService.updateShipmentStatus(10L, reqInTransit))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_SHIPMENT_STATUS_TRANSITION);

    // Thử nhảy cóc từ READY_TO_PICK sang DELIVERED
    ShipmentStatusUpdateRequest reqDelivered =
        ShipmentStatusUpdateRequest.builder().status("DELIVERED").build();

    assertThatThrownBy(() -> shipmentsService.updateShipmentStatus(10L, reqDelivered))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_SHIPMENT_STATUS_TRANSITION);
  }

  @Test
  void updateShipmentStatus_BackwardStep_ThrowsException() {
    shipment.setShippingStatus("IN_TRANSIT");
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

    // Thử lùi lại từ IN_TRANSIT về PICKED
    ShipmentStatusUpdateRequest req =
        ShipmentStatusUpdateRequest.builder().status("PICKED").build();

    assertThatThrownBy(() -> shipmentsService.updateShipmentStatus(10L, req))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_SHIPMENT_STATUS_TRANSITION);
  }

  @Test
  void updateShipmentStatus_StepByStep_Success() {
    shipment.setShippingStatus("READY_TO_PICK");
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

    // Chuyển đúng 1 bước: READY_TO_PICK -> PICKED
    ShipmentStatusUpdateRequest reqPicked =
        ShipmentStatusUpdateRequest.builder().status("PICKED").build();

    ShipmentStatusSyncResponse res = shipmentsService.updateShipmentStatus(10L, reqPicked);

    assertThat(res).isNotNull();
    assertThat(res.getShipmentStatus()).isEqualTo("PICKED");
    assertThat(res.getSynchronizedOrderStatus()).isEqualTo("PICKED_UP");
    verify(shipmentRepository).save(shipment);
  }

  @Test
  void failDelivery_WhenReadyToPick_ThrowsException() {
    shipment.setShippingStatus("READY_TO_PICK");
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

    ShipmentFailDeliveryRequest req =
        ShipmentFailDeliveryRequest.builder().failReason("Chưa giao mà đã báo fail").build();

    assertThatThrownBy(() -> shipmentsService.failDelivery(10L, req))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_SHIPMENT_STATUS_TRANSITION);
  }
}
