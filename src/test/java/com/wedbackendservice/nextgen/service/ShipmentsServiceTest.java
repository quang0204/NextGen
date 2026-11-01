package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.request.shipment.ShipmentStatusUpdateRequest;
import com.wedbackendservice.nextgen.dto.response.shipment.AdminShipmentListResponse;
import com.wedbackendservice.nextgen.dto.response.shipment.ShipmentStatusSyncResponse;
import com.wedbackendservice.nextgen.dto.response.shipment.ShipmentTrackResponse;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShipmentsServiceTest {

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

    @InjectMocks
    private ShipmentsService shipmentsService;

    private User staffUser;
    private Order order;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        staffUser = User.builder()
                .userId(2L)
                .fullName("Nhân viên Kho")
                .role(Role.STAFF)
                .status(UserStatus.ACTIVE)
                .build();

        order = Order.builder()
                .orderId(10L)
                .orderCode("DH-20261015-089")
                .recipientName("Trần Thị Thu Huyền")
                .recipientPhone("0987654321")
                .shippingAddress("123 Lê Duẩn, Đà Nẵng")
                .status("CONFIRMED")
                .totalAmount(new BigDecimal("15000000"))
                .build();

        shipment = Shipment.builder()
                .shipmentId(89L)
                .trackingNumber("GHN-20261015089")
                .carrierName("Giao Hàng Nhanh Express")
                .shippingStatus("IN_TRANSIT")
                .order(order)
                .estimatedDelivery(LocalDateTime.now().plusDays(2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testTrackShipment_Success() {
        when(shipmentRepository.findByTrackingCodeOrOrderCode("GHN-20261015089")).thenReturn(Optional.of(shipment));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of());

        ShipmentTrackResponse resp = shipmentsService.trackShipment("GHN-20261015089");

        assertNotNull(resp);
        assertEquals(89L, resp.getShipmentId());
        assertEquals("GHN-20261015089", resp.getTrackingCode());
        assertEquals("IN_TRANSIT", resp.getStatus());
        assertEquals("Giao Hàng Nhanh Express", resp.getCarrier());
    }

    @Test
    void testTrackShipment_NotFound_ThrowsException() {
        when(shipmentRepository.findByTrackingCodeOrOrderCode("UNKNOWN-123")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> shipmentsService.trackShipment("UNKNOWN-123"));
        assertEquals(ErrorCode.SHIPMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void testUpdateShipmentStatus_Delivered_Success() {
        when(securityHelper.authenticateStaffOrAdmin("Bearer staff-token")).thenReturn(staffUser);
        when(shipmentRepository.findById(89L)).thenReturn(Optional.of(shipment));
        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());

        ShipmentStatusUpdateRequest req = ShipmentStatusUpdateRequest.builder()
                .status("DELIVERED")
                .note("Khách hàng đã nhận được kiện hàng nguyên vẹn")
                .build();

        ShipmentStatusSyncResponse resp = shipmentsService.updateShipmentStatus("Bearer staff-token", 89L, req);

        assertNotNull(resp);
        assertEquals(89L, resp.getShipmentId());
        assertEquals("DELIVERED", resp.getShipmentStatus());
        assertEquals("COMPLETED", resp.getSynchronizedOrderStatus());
        assertTrue(resp.getIsPaymentCompleted());
        verify(shipmentRepository, times(1)).save(shipment);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void testGetAdminShipments_Success() {
        when(securityHelper.authenticateStaffOrAdmin("Bearer staff-token")).thenReturn(staffUser);
        Page<Shipment> page = new PageImpl<>(List.of(shipment));
        when(shipmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AdminShipmentListResponse resp = shipmentsService.getAdminShipments(
                "Bearer staff-token", 1, 10, "ALL", "ALL", null);

        assertNotNull(resp);
        assertEquals(1, resp.getItems().size());
        assertEquals(89L, resp.getItems().get(0).getShipmentId());
        assertEquals(1, resp.getPagination().getTotalItems());
    }
}
