package com.wedbackendservice.nextgen.service.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.dto.client.request.returnrequest.BankInfoRequest;
import com.wedbackendservice.nextgen.dto.client.request.returnrequest.CreateReturnRequest;
import com.wedbackendservice.nextgen.dto.client.response.returnrequest.CreateReturnResponse;
import com.wedbackendservice.nextgen.dto.portal.request.returnrequest.InspectReturnRequest;
import com.wedbackendservice.nextgen.dto.portal.request.returnrequest.RestockItemRequest;
import com.wedbackendservice.nextgen.dto.portal.response.returnrequest.AdminReturnListResponse;
import com.wedbackendservice.nextgen.dto.portal.response.returnrequest.InspectReturnResponse;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnsServiceTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SecurityHelper securityHelper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ReturnsService returnsService;

    private User testUser;
    private User staffUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(105L)
                .fullName("Trần Thu Huyền")
                .phone("0987654321")
                .email("huyen@gmail.com")
                .role(Role.CUSTOMER)
                .build();

        staffUser = User.builder()
                .userId(201L)
                .fullName("Hoàng Kỹ Thuật")
                .email("hoang.kythuat@phoneshop.vn")
                .role(Role.STAFF)
                .build();

        testOrder = Order.builder()
                .orderId(210L)
                .orderCode("ORD-20261102-004")
                .user(testUser)
                .status("COMPLETED")
                .paymentMethod("BANK_TRANSFER")
                .paymentStatus("PAID")
                .completedAt(LocalDateTime.now().minusDays(5))
                .build();
    }

    @Test
    @DisplayName("Tạo yêu cầu đổi trả thành công trong vòng 30 ngày")
    void createReturn_Success() {
        when(securityHelper.getCurrentUser()).thenReturn(testUser);
        when(orderRepository.findById(210L)).thenReturn(Optional.of(testOrder));
        when(returnRequestRepository.existsByOrder_OrderIdAndStatusIn(eq(210L), anyCollection())).thenReturn(false);

        ReturnRequest saved = ReturnRequest.builder()
                .returnId(35L)
                .order(testOrder)
                .requestedBy(testUser)
                .reason("Màn hình xuất hiện sọc xanh sau 5 ngày sử dụng")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        when(returnRequestRepository.save(any(ReturnRequest.class))).thenReturn(saved);

        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(210L)
                .reason("Màn hình xuất hiện sọc xanh sau 5 ngày sử dụng")
                .evidenceImages(List.of("https://phoneshop.vn/uploads/soc.jpg"))
                .build();

        CreateReturnResponse response = returnsService.createReturn(request);

        assertThat(response).isNotNull();
        assertThat(response.getReturnId()).isEqualTo(35L);
        assertThat(response.getOrderCode()).isEqualTo("ORD-20261102-004");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getRequestedBy()).isEqualTo(105L);
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    @DisplayName("Từ chối khi đơn hàng đã hoàn thành quá 30 ngày (RETURN_PERIOD_EXPIRED)")
    void createReturn_PeriodExpired() {
        testOrder.setCompletedAt(LocalDateTime.now().minusDays(31));

        when(securityHelper.getCurrentUser()).thenReturn(testUser);
        when(orderRepository.findById(210L)).thenReturn(Optional.of(testOrder));

        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(210L)
                .reason("Màn hình xuất hiện sọc xanh sau 35 ngày sử dụng")
                .evidenceImages(List.of("https://phoneshop.vn/uploads/soc.jpg"))
                .build();

        assertThatThrownBy(() -> returnsService.createReturn(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.RETURN_PERIOD_EXPIRED));
    }

    @Test
    @DisplayName("Từ chối khi đơn hàng chưa COMPLETED (RETURN_ORDER_NOT_COMPLETED)")
    void createReturn_OrderNotCompleted() {
        testOrder.setStatus("SHIPPING");

        when(securityHelper.getCurrentUser()).thenReturn(testUser);
        when(orderRepository.findById(210L)).thenReturn(Optional.of(testOrder));

        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(210L)
                .reason("Màn hình xuất hiện sọc xanh khi chưa nhận")
                .evidenceImages(List.of("https://phoneshop.vn/uploads/soc.jpg"))
                .build();

        assertThatThrownBy(() -> returnsService.createReturn(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.RETURN_ORDER_NOT_COMPLETED));
    }

    @Test
    @DisplayName("Từ chối khi đơn hàng đã có yêu cầu đổi trả đang xử lý (DUPLICATE_RETURN_REQUEST)")
    void createReturn_DuplicateRequest() {
        when(securityHelper.getCurrentUser()).thenReturn(testUser);
        when(orderRepository.findById(210L)).thenReturn(Optional.of(testOrder));
        when(returnRequestRepository.existsByOrder_OrderIdAndStatusIn(eq(210L), anyCollection())).thenReturn(true);

        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(210L)
                .reason("Màn hình xuất hiện sọc xanh sau 5 ngày sử dụng")
                .evidenceImages(List.of("https://phoneshop.vn/uploads/soc.jpg"))
                .build();

        assertThatThrownBy(() -> returnsService.createReturn(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RETURN_REQUEST));
    }

    @Test
    @DisplayName("Bắt buộc thông tin bankInfo nếu phương thức thanh toán ban đầu là COD")
    void createReturn_CodRequiresBankInfo() {
        testOrder.setPaymentMethod("COD");

        when(securityHelper.getCurrentUser()).thenReturn(testUser);
        when(orderRepository.findById(210L)).thenReturn(Optional.of(testOrder));
        when(returnRequestRepository.existsByOrder_OrderIdAndStatusIn(eq(210L), anyCollection())).thenReturn(false);

        CreateReturnRequest requestWithoutBank = CreateReturnRequest.builder()
                .orderId(210L)
                .reason("Màn hình xuất hiện sọc xanh sau 5 ngày sử dụng")
                .evidenceImages(List.of("https://phoneshop.vn/uploads/soc.jpg"))
                .build();

        assertThatThrownBy(() -> returnsService.createReturn(requestWithoutBank))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.RETURN_BANK_INFO_REQUIRED));
    }

    @Test
    @DisplayName("Thẩm định Chấp thuận (APPROVED) và lập biên bản hoàn kho MB04")
    void inspectReturn_Approved() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

        ReturnRequest pendingReturn = ReturnRequest.builder()
                .returnId(35L)
                .order(testOrder)
                .requestedBy(testUser)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(pendingReturn));

        ProductVariant variant = ProductVariant.builder()
                .variantId(101L)
                .sku("IP15PM-256-TI")
                .build();
        when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variant));

        Inventory inventory = Inventory.builder()
                .inventoryId(1L)
                .variant(variant)
                .quantity(8)
                .reservedQuantity(5)
                .minThreshold(5)
                .build();
        when(inventoryRepository.findByVariant_VariantId(101L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        InspectReturnRequest request = InspectReturnRequest.builder()
                .status("APPROVED")
                .inspectionResult("Xác nhận lỗi panel")
                .refundAmount(BigDecimal.valueOf(29990000))
                .restockItems(List.of(
                        RestockItemRequest.builder().variantId(101L).quantity(1).build()
                ))
                .build();

        InspectReturnResponse response = returnsService.inspectReturn(35L, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getInspectedBy()).isEqualTo("hoang.kythuat@phoneshop.vn");
        assertThat(response.getRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(29990000));
        assertThat(response.getInventoryRestock()).isNotNull();
        assertThat(response.getInventoryRestock().getFormCode()).isEqualTo("MB04");
        assertThat(response.getInventoryRestock().getTxnType()).isEqualTo("RETURN");
        assertThat(response.getInventoryRestock().getRestockedItems()).hasSize(1);
        assertThat(response.getInventoryRestock().getRestockedItems().get(0).getNewOnHandQty()).isEqualTo(9);
        assertThat(response.getInventoryRestock().getRestockedItems().get(0).getNewAvailableQty()).isEqualTo(4);

        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Thẩm định Từ chối (REJECTED) không tác động kho")
    void inspectReturn_Rejected() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

        ReturnRequest pendingReturn = ReturnRequest.builder()
                .returnId(35L)
                .order(testOrder)
                .requestedBy(testUser)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(pendingReturn));

        InspectReturnRequest request = InspectReturnRequest.builder()
                .status("REJECTED")
                .inspectionResult("Quỳ tím đổi màu đỏ")
                .rejectReason("Thiết bị vào nước")
                .build();

        InspectReturnResponse response = returnsService.inspectReturn(35L, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectReason()).isEqualTo("Thiết bị vào nước");
        assertThat(response.getInventoryRestocked()).isFalse();

        verifyNoInteractions(inventoryRepository);
        verifyNoInteractions(inventoryTransactionRepository);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Thẩm định thất bại nếu yêu cầu đã được xử lý trước đó (RETURN_ALREADY_PROCESSED)")
    void inspectReturn_AlreadyProcessed() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

        ReturnRequest approvedReturn = ReturnRequest.builder()
                .returnId(35L)
                .order(testOrder)
                .requestedBy(testUser)
                .status("APPROVED")
                .build();

        when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(approvedReturn));

        InspectReturnRequest request = InspectReturnRequest.builder()
                .status("REJECTED")
                .inspectionResult("Thẩm định lại")
                .rejectReason("Lý do khác")
                .build();

        assertThatThrownBy(() -> returnsService.inspectReturn(35L, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.RETURN_ALREADY_PROCESSED));
    }

    @Test
    @DisplayName("Lấy danh sách yêu cầu đổi trả phân trang và thống kê")
    void getAdminReturns_Success() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

        ReturnRequest ret = ReturnRequest.builder()
                .returnId(35L)
                .order(testOrder)
                .requestedBy(testUser)
                .reason("Màn hình sọc")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        when(returnRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ret)));
        when(returnRequestRepository.countByStatusIn(anyCollection())).thenReturn(5L);
        when(returnRequestRepository.countByStatus("APPROVED")).thenReturn(42L);
        when(returnRequestRepository.countByStatus("REJECTED")).thenReturn(8L);

        AdminReturnListResponse response = returnsService.getAdminReturns(1, 20, "PENDING", null, "createdAt", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getSummary().getTotalPending()).isEqualTo(5L);
        assertThat(response.getSummary().getTotalApproved()).isEqualTo(42L);
        assertThat(response.getSummary().getTotalRejected()).isEqualTo(8L);
        assertThat(response.getPagination().getTotalItems()).isEqualTo(1L);
    }
}
