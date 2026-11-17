package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.portal.request.order.PackExportOrderRequest;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.wedbackendservice.nextgen.mapper.OrderMapper;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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
    private ReturnRequestRepository returnRequestRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private TokenRedisService tokenRedisService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private ShippingFeeCalculator shippingFeeCalculator;
    @Mock
    private OrderExpirationService orderExpirationService;
    @Mock
    private OrderPostCommitService orderPostCommitService;
    @Spy
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();
    @Spy
    private OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    @InjectMocks
    private OrdersService ordersService;

    @Test
    void packAndExportOrder_throwsWhenAtomicStockDeductionReturnsZero() {
        User staff = User.builder().userId(2L).fullName("Staff").role(Role.STAFF).build();
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staff);

        Order order = Order.builder().orderId(10L).orderCode("DH10").status("CONFIRMED").build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        when(inventoryService.exportStockForOrderWithDetails(eq(10L), eq(staff), any())).thenReturn(java.util.List.of());

        assertThatThrownBy(() -> ordersService.packAndExportOrder(10L, new PackExportOrderRequest()))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_STOCK_INSUFFICIENT);
    }

    @Test
    void customerNotifyPayment_rejectsNonOwnerCustomer() {
        User attacker = User.builder().userId(99L).fullName("Other").role(Role.CUSTOMER).build();
        User owner = User.builder().userId(10L).fullName("Owner").role(Role.CUSTOMER).build();
        Order order = Order.builder().orderId(500L).orderCode("DH500").user(owner).paymentStatus("UNPAID").build();

        when(securityHelper.getCurrentUser()).thenReturn(attacker);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> ordersService.customerNotifyPayment(500L, "Đã chuyển khoản"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
