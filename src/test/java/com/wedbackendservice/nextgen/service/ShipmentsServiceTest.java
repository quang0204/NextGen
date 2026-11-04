package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.shipment.AdminShipmentItemResponse;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.Payment;
import com.wedbackendservice.nextgen.entity.Shipment;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ShipmentsService shipmentsService;

    @Test
    void getAdminShipments_InvalidPage_ThrowsAppException() {
        assertThatThrownBy(() -> shipmentsService.getAdminShipments(0, 10, null, null, null))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_NUMBER);
    }

    @Test
    void getAdminShipments_InvalidSize_ThrowsAppException() {
        assertThatThrownBy(() -> shipmentsService.getAdminShipments(1, 999, null, null, null))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_SIZE);
    }

    @Test
    void getAdminShipments_ValidPagination_ExecutesBatchPaymentQuery() {
        Order order1 = Order.builder().orderId(101L).orderCode("DH-101").finalAmount(new BigDecimal("500000")).build();
        Order order2 = Order.builder().orderId(102L).orderCode("DH-102").finalAmount(new BigDecimal("750000")).build();

        Shipment shipment1 = Shipment.builder()
                .shipmentId(1L)
                .order(order1)
                .trackingNumber("VN001")
                .carrierName("GHN")
                .shippingStatus("DELIVERED")
                .createdAt(LocalDateTime.now())
                .build();

        Shipment shipment2 = Shipment.builder()
                .shipmentId(2L)
                .order(order2)
                .trackingNumber("VN002")
                .carrierName("GHTK")
                .shippingStatus("IN_TRANSIT")
                .createdAt(LocalDateTime.now())
                .build();

        Page<Shipment> paged = new PageImpl<>(List.of(shipment1, shipment2));
        when(shipmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(paged);

        Payment payment1 = Payment.builder().order(order1).paymentMethod("VNPAY").createdAt(LocalDateTime.now()).build();
        Payment payment2 = Payment.builder().order(order2).paymentMethod("COD").createdAt(LocalDateTime.now()).build();
        when(paymentRepository.findByOrderIdsOrderByCreatedAtDesc(List.of(101L, 102L))).thenReturn(List.of(payment1, payment2));

        PageResponse<AdminShipmentItemResponse> result = shipmentsService.getAdminShipments(1, 10, null, null, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getPaymentMethod()).isEqualTo("VNPAY");
        assertThat(result.getContent().get(1).getPaymentMethod()).isEqualTo("COD");
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);

        // Batch query for payment executed once, no N+1
        verify(paymentRepository, times(1)).findByOrderIdsOrderByCreatedAtDesc(List.of(101L, 102L));
    }
}
