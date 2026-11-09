package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.request.order.CancelOrderRequest;
import com.wedbackendservice.nextgen.dto.client.request.order.CreateOrderRequest;
import com.wedbackendservice.nextgen.dto.client.request.order.OrderItemRequest;
import com.wedbackendservice.nextgen.dto.client.response.order.CancelOrderResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.CreateOrderResponse;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private PromotionVariantRepository promotionVariantRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private TokenRedisService tokenRedisService;

    @InjectMocks
    private OrdersService ordersService;

    private User testUser;
    private ProductVariant variant1;
    private Inventory inventory1;

    @BeforeEach
    void setUp() {
        testUser = User.builder().userId(10L).fullName("Test User").role(Role.CUSTOMER).build();

        Product product = Product.builder().productId(100L).productName("iPhone 15").build();
        variant1 = ProductVariant.builder()
                .variantId(1L)
                .product(product)
                .sku("IPHONE15-128-BLK")
                .price(BigDecimal.valueOf(20000000))
                .build();

        inventory1 = Inventory.builder()
                .inventoryId(1L)
                .variant(variant1)
                .quantity(10)
                .reservedQuantity(0)
                .build();
    }

    @Test
    @DisplayName("createOrder merges duplicate variant items and reserves aggregated quantity")
    void createOrder_shouldMergeDuplicateVariantItems() {
        when(securityHelper.getCurrentUser()).thenReturn(testUser);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant1));
        when(inventoryRepository.findByVariant_VariantId(1L)).thenReturn(Optional.of(inventory1));
        when(inventoryRepository.reserveStockAtomic(eq(1L), eq(3), any())).thenReturn(1);

        Order savedMock = Order.builder()
                .orderId(500L)
                .orderCode("DH-20261109-123456789")
                .status("WAITING_CONFIRMATION")
                .totalAmount(BigDecimal.valueOf(60000000))
                .finalAmount(BigDecimal.valueOf(60000000))
                .paymentMethod("COD")
                .paymentStatus("UNPAID")
                .createdAt(LocalDateTime.now())
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedMock);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .shippingAddress("123 Duong ABC, Quan 1, TP.HCM")
                .paymentMethod("COD")
                .items(List.of(
                        OrderItemRequest.builder().variantId(1L).quantity(1).build(),
                        OrderItemRequest.builder().variantId(1L).quantity(2).build()
                ))
                .build();

        CreateOrderResponse response = ordersService.createOrder(request, null);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(500L);
        // Verify reserveStockAtomic was called with aggregated qty = 3 once
        verify(inventoryRepository, times(1)).reserveStockAtomic(eq(1L), eq(3), any());
    }

    @Test
    @DisplayName("createOrder with idempotencyKey returns existing cached order")
    void createOrder_shouldReturnCachedOrderOnDuplicateKey() {
        when(securityHelper.getCurrentUser()).thenReturn(testUser);
        when(tokenRedisService.getValue("idempotency:order:10:idem-key-123")).thenReturn("DH-EXISTING-001");

        Order existingOrder = Order.builder()
                .orderId(999L)
                .orderCode("DH-EXISTING-001")
                .status("WAITING_CONFIRMATION")
                .totalAmount(BigDecimal.valueOf(20000000))
                .finalAmount(BigDecimal.valueOf(20000000))
                .paymentMethod("COD")
                .paymentStatus("UNPAID")
                .createdAt(LocalDateTime.now())
                .build();
        when(orderRepository.findByOrderCode("DH-EXISTING-001")).thenReturn(Optional.of(existingOrder));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .shippingAddress("123 Duong ABC")
                .items(List.of(OrderItemRequest.builder().variantId(1L).quantity(1).build()))
                .build();

        CreateOrderResponse response = ordersService.createOrder(request, "idem-key-123");

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(999L);
        assertThat(response.getOrderCode()).isEqualTo("DH-EXISTING-001");
        // Verify no new reservation or order creation was made
        verify(orderRepository, never()).save(any(Order.class));
        verify(inventoryRepository, never()).reserveStockAtomic(anyLong(), anyInt(), any());
    }

    @Test
    @DisplayName("cancelOrder triggers atomic inventory release and voucher decrement")
    void cancelOrder_shouldReleaseStockAndDecrementVoucherAtomically() {
        when(securityHelper.getCurrentUser()).thenReturn(testUser);

        Voucher voucher = Voucher.builder().voucherId(5L).voucherCode("DISCOUNT10").usedCount(2).build();
        Order order = Order.builder()
                .orderId(123L)
                .orderCode("DH-123")
                .status("WAITING_CONFIRMATION")
                .user(testUser)
                .voucher(voucher)
                .build();
        when(orderRepository.findByOrderCode("DH-123")).thenReturn(Optional.of(order));

        OrderItem item = OrderItem.builder().orderItemId(1L).variant(variant1).quantity(2).build();
        when(orderItemRepository.findByOrderId(123L)).thenReturn(List.of(item));

        CancelOrderRequest cancelReq = CancelOrderRequest.builder().reason("Khong con nhu cau").build();
        CancelOrderResponse cancelRes = ordersService.cancelOrder("DH-123", cancelReq);

        assertThat(cancelRes).isNotNull();
        assertThat(cancelRes.getStatus()).isEqualTo("CANCELLED");

        // Verify atomic inventory release
        verify(inventoryRepository).releaseStockAtomic(eq(1L), eq(2), any());
        // Verify atomic voucher decrement
        verify(voucherRepository).decrementUsedCountAtomic(5L);
    }
}
