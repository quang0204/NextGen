package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.response.shipment.ShipmentTrackResponse;
import com.wedbackendservice.nextgen.dto.portal.request.shipment.ShipmentFailDeliveryRequest;
import com.wedbackendservice.nextgen.dto.portal.request.shipment.ShipmentStatusUpdateRequest;
import com.wedbackendservice.nextgen.dto.portal.response.shipment.ShipmentFailDeliveryResponse;
import com.wedbackendservice.nextgen.dto.portal.response.shipment.ShipmentStatusSyncResponse;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.wedbackendservice.nextgen.mapper.ShipmentMapper;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentsServiceTest {

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Spy
    private ShipmentMapper shipmentMapper = Mappers.getMapper(ShipmentMapper.class);

    @InjectMocks
    private ShipmentsService shipmentsService;

    private User staffUser;
    private User customer;
    private Order order;
    private Shipment shipment;
    private ProductVariant variant;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        staffUser = User.builder()
                .userId(1L)
                .fullName("Staff Nguyen")
                .role(Role.STAFF)
                .build();

        customer = User.builder()
                .userId(2L)
                .fullName("Nguyen Van A")
                .phone("0912345678")
                .build();

        order = Order.builder()
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

        shipment = Shipment.builder()
                .shipmentId(10L)
                .order(order)
                .trackingNumber("VN123456789")
                .carrierName("GHTK")
                .shippingStatus("IN_TRANSIT")
                .recipientName("Nguyen Van A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Nguyen Trai, Q5, TPHCM")
                .build();

        variant = ProductVariant.builder()
                .variantId(50L)
                .sku("SKU-50")
                .build();

        orderItem = OrderItem.builder()
                .orderItemId(500L)
                .order(order)
                .variant(variant)
                .quantity(2)
                .build();
    }

    @Test
    void trackShipment_Success_MasksPII() {
        when(shipmentRepository.findByTrackingCodeOrOrderCode("VN123456789")).thenReturn(Optional.of(shipment));
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
        when(shipmentRepository.findByTrackingCodeOrOrderCode("NOT_FOUND")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentsService.trackShipment("NOT_FOUND"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SHIPMENT_NOT_FOUND);
    }

    @Test
    void updateShipmentStatus_Delivered_CompletesOrderAndPayment() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

        Payment payment = Payment.builder()
                .paymentId(1L)
                .order(order)
                .status("PENDING")
                .build();
        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.of(payment));

        ShipmentStatusUpdateRequest req = ShipmentStatusUpdateRequest.builder()
                .status("DELIVERED")
                .note("Khách đã nhận hàng")
                .build();

        ShipmentStatusSyncResponse res = shipmentsService.updateShipmentStatus(10L, req);

        assertThat(res).isNotNull();
        assertThat(res.getShipmentStatus()).isEqualTo("DELIVERED");
        assertThat(res.getSynchronizedOrderStatus()).isEqualTo("COMPLETED");
        assertThat(res.getIsPaymentCompleted()).isTrue();

        assertThat(order.getStatus()).isEqualTo("COMPLETED");
        assertThat(payment.getStatus()).isEqualTo("PAID");
        verify(shipmentRepository).save(shipment);
        verify(orderRepository, atLeastOnce()).save(order);
        verify(paymentRepository).save(payment);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void failDelivery_Success_RestocksInventory() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(orderItem));
        when(inventoryRepository.increaseQuantityAtomic(eq(50L), eq(2), any(LocalDateTime.class))).thenReturn(1);

        ShipmentFailDeliveryRequest req = ShipmentFailDeliveryRequest.builder()
                .failReason("Khách không nghe máy sau 3 lần gọi")
                .build();

        ShipmentFailDeliveryResponse res = shipmentsService.failDelivery(10L, req);

        assertThat(res).isNotNull();
        assertThat(res.getShipmentStatus()).isEqualTo("FAILED_DELIVERY");
        assertThat(res.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(res.getInventoryRestocked()).isTrue();
        assertThat(res.getRestockedItemsCount()).isEqualTo(1);

        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        verify(inventoryRepository).increaseQuantityAtomic(eq(50L), eq(2), any(LocalDateTime.class));
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void failDelivery_AlreadyDelivered_ThrowsInvalidRequest() {
        shipment.setShippingStatus("DELIVERED");
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));

        ShipmentFailDeliveryRequest req = ShipmentFailDeliveryRequest.builder()
                .failReason("Hàng hỏng")
                .build();

        assertThatThrownBy(() -> shipmentsService.failDelivery(10L, req))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(inventoryRepository, never()).increaseQuantityAtomic(anyLong(), anyInt(), any());
    }
}
