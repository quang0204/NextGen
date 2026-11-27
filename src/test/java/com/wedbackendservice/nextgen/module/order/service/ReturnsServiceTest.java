package com.wedbackendservice.nextgen.module.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.outbox.service.OutboxService;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.module.inventory.entity.Inventory;
import com.wedbackendservice.nextgen.module.inventory.entity.InventoryTransaction;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryTransactionRepository;
import com.wedbackendservice.nextgen.module.notification.entity.Notification;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.order.dto.client.request.BankInfoRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.request.CreateReturnRequest;
import com.wedbackendservice.nextgen.module.order.dto.client.response.CreateReturnResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.request.InspectReturnRequest;
import com.wedbackendservice.nextgen.module.order.dto.portal.request.RestockItemRequest;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.AdminReturnListResponse;
import com.wedbackendservice.nextgen.module.order.dto.portal.response.InspectReturnResponse;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.entity.OrderItem;
import com.wedbackendservice.nextgen.module.order.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.module.order.entity.ReturnRequest;
import com.wedbackendservice.nextgen.module.order.repository.OrderItemRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.order.repository.ReturnRequestRepository;
import com.wedbackendservice.nextgen.module.payment.entity.Payment;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.product.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.voucher.entity.Voucher;
import com.wedbackendservice.nextgen.module.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class ReturnsServiceTest {

  @Mock private ReturnRequestRepository returnRequestRepository;

  @Mock private OrderRepository orderRepository;

  @Mock private OrderItemRepository orderItemRepository;

  @Mock private InventoryRepository inventoryRepository;

  @Mock private InventoryTransactionRepository inventoryTransactionRepository;

  @Mock private ProductVariantRepository productVariantRepository;

  @Mock private PaymentRepository paymentRepository;

  @Mock private AuditLogRepository auditLogRepository;

  @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;

  @Mock private NotificationRepository notificationRepository;

  @Mock private VoucherRepository voucherRepository;

  @Mock private SecurityHelper securityHelper;

  @Mock private OutboxService outboxService;

  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private ReturnsService returnsService;

  private User testUser;
  private User staffUser;
  private Order testOrder;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .userId(105L)
            .fullName("Trần Thu Huyền")
            .phone("0987654321")
            .email("huyen@gmail.com")
            .role(Role.CUSTOMER)
            .build();

    staffUser =
        User.builder()
            .userId(201L)
            .fullName("Hoàng Kỹ Thuật")
            .email("hoang.kythuat@phoneshop.vn")
            .role(Role.STAFF)
            .build();

    testOrder =
        Order.builder()
            .orderId(210L)
            .orderCode("ORD-20261102-004")
            .user(testUser)
            .status("COMPLETED")
            .paymentMethod("BANK_TRANSFER")
            .paymentStatus("PAID")
            .totalAmount(BigDecimal.valueOf(29990000))
            .finalAmount(BigDecimal.valueOf(29990000))
            .completedAt(LocalDateTime.now().minusDays(5))
            .build();
  }

  @Test
  @DisplayName("Tạo yêu cầu đổi trả thành công trong vòng 30 ngày")
  void createReturn_Success() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);
    when(orderRepository.findById(210L)).thenReturn(Optional.of(testOrder));
    when(returnRequestRepository.existsByOrder_OrderIdAndStatusIn(eq(210L), anyCollection()))
        .thenReturn(false);

    ReturnRequest saved =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .reason("Màn hình xuất hiện sọc xanh sau 5 ngày sử dụng")
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();

    when(returnRequestRepository.save(any(ReturnRequest.class))).thenReturn(saved);

    CreateReturnRequest request =
        CreateReturnRequest.builder()
            .orderId(210L)
            .reason("Màn hình xuất hiện sọc xanh sau 5 ngày sử dụng")
            .evidenceImages(List.of("https://phoneshop.vn/uploads/soc.jpg"))
            .build();

    CreateReturnResponse response = returnsService.createReturn(request);

    assertThat(response).isNotNull();
    assertThat(response.getReturnId()).isEqualTo(35L);
    assertThat(response.getStatus()).isEqualTo("PENDING");
    assertThat(response.getOrderCode()).isEqualTo("ORD-20261102-004");
    assertThat(response.getRequestedBy()).isEqualTo(105L);
    verify(auditLogRepository, times(1))
        .log(
            eq(testUser), eq("CREATE_RETURN_REQUEST"), anyString(), eq(35L), isNull(), anyString());
  }

  @Test
  @DisplayName("Tạo yêu cầu đổi trả thất bại khi đơn hàng quá hạn 30 ngày")
  void createReturn_Expired() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);

    Order oldOrder =
        Order.builder()
            .orderId(211L)
            .user(testUser)
            .status("COMPLETED")
            .completedAt(LocalDateTime.now().minusDays(35))
            .build();
    when(orderRepository.findById(211L)).thenReturn(Optional.of(oldOrder));

    CreateReturnRequest request =
        CreateReturnRequest.builder()
            .orderId(211L)
            .reason("Máy pin tụt nhanh")
            .evidenceImages(List.of("https://phoneshop.vn/uploads/pin.jpg"))
            .build();

    assertThatThrownBy(() -> returnsService.createReturn(request))
        .isInstanceOf(AppException.class)
        .satisfies(
            e ->
                assertThat(((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RETURN_PERIOD_EXPIRED));
  }

  @Test
  @DisplayName("Tạo yêu cầu đổi trả đơn hàng COD bắt buộc có thông tin tài khoản ngân hàng")
  void createReturn_CodRequiresBankInfo() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);

    Order codOrder =
        Order.builder()
            .orderId(212L)
            .user(testUser)
            .status("COMPLETED")
            .paymentMethod("COD")
            .completedAt(LocalDateTime.now().minusDays(2))
            .build();
    when(orderRepository.findById(212L)).thenReturn(Optional.of(codOrder));
    when(returnRequestRepository.existsByOrder_OrderIdAndStatusIn(eq(212L), anyCollection()))
        .thenReturn(false);

    CreateReturnRequest request =
        CreateReturnRequest.builder()
            .orderId(212L)
            .reason("Máy bị lỗi loa trong")
            .evidenceImages(List.of("https://phoneshop.vn/uploads/loa.jpg"))
            .bankInfo(null)
            .build();

    assertThatThrownBy(() -> returnsService.createReturn(request))
        .isInstanceOf(AppException.class)
        .satisfies(
            e ->
                assertThat(((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RETURN_BANK_INFO_REQUIRED));
  }

  @Test
  @DisplayName("Tạo yêu cầu đổi trả COD hợp lệ khi có đầy đủ tài khoản ngân hàng")
  void createReturn_CodWithBankInfo_Success() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);

    Order codOrder =
        Order.builder()
            .orderId(212L)
            .orderCode("ORD-COD-001")
            .user(testUser)
            .status("COMPLETED")
            .paymentMethod("COD")
            .completedAt(LocalDateTime.now().minusDays(2))
            .build();
    when(orderRepository.findById(212L)).thenReturn(Optional.of(codOrder));
    when(returnRequestRepository.existsByOrder_OrderIdAndStatusIn(eq(212L), anyCollection()))
        .thenReturn(false);

    ReturnRequest saved =
        ReturnRequest.builder()
            .returnId(36L)
            .order(codOrder)
            .requestedBy(testUser)
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();
    when(returnRequestRepository.save(any(ReturnRequest.class))).thenReturn(saved);

    CreateReturnRequest request =
        CreateReturnRequest.builder()
            .orderId(212L)
            .reason("Máy bị lỗi loa trong")
            .evidenceImages(List.of("https://phoneshop.vn/uploads/loa.jpg"))
            .bankInfo(
                BankInfoRequest.builder()
                    .bankName("Vietcombank")
                    .accountNumber("0123456789")
                    .accountHolder("TRAN THU HUYEN")
                    .build())
            .build();

    CreateReturnResponse response = returnsService.createReturn(request);

    assertThat(response).isNotNull();
    assertThat(response.getReturnId()).isEqualTo(36L);
  }

  @Test
  @DisplayName(
      "Tạo yêu cầu đổi trả cho sản phẩm thứ 2 thành công khi sản phẩm thứ 1 đã có phiếu hoàn")
  void createReturn_SecondItem_Success() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);
    when(orderRepository.findById(210L)).thenReturn(Optional.of(testOrder));

    ReturnRequest firstItemReturn =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .reason("[Yêu cầu hoàn trả một phần: Điện thoại iphone 19 (x1)] - Lỗi màn hình")
            .requestedItems(
                "[{\"variantId\":101,\"productName\":\"Điện thoại iphone 19\",\"sku\":\"IPS3\",\"quantity\":1,\"unitPrice\":28000000}]")
            .status("APPROVED")
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();

    when(returnRequestRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(anyCollection()))
        .thenReturn(List.of(firstItemReturn));

    ReturnRequest secondItemReturn =
        ReturnRequest.builder()
            .returnId(36L)
            .order(testOrder)
            .requestedBy(testUser)
            .reason("[Yêu cầu hoàn trả một phần: iPhone 15 Pro Max (x1)] - Lỗi pin")
            .requestedItems(
                "[{\"variantId\":102,\"productName\":\"iPhone 15 Pro Max\",\"sku\":\"IP15PM-256-TI\",\"quantity\":1,\"unitPrice\":28990000}]")
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();

    when(returnRequestRepository.save(any(ReturnRequest.class))).thenReturn(secondItemReturn);

    CreateReturnRequest request =
        CreateReturnRequest.builder()
            .orderId(210L)
            .reason("[Yêu cầu hoàn trả một phần: iPhone 15 Pro Max (x1)] - Lỗi pin")
            .evidenceImages(List.of("https://phoneshop.vn/uploads/pin.jpg"))
            .items(
                List.of(
                    com.wedbackendservice.nextgen.module.order.dto.client.request.ReturnItemRequest
                        .builder()
                        .variantId(102L)
                        .productName("iPhone 15 Pro Max")
                        .sku("IP15PM-256-TI")
                        .quantity(1)
                        .unitPrice(BigDecimal.valueOf(28990000))
                        .build()))
            .build();

    CreateReturnResponse response = returnsService.createReturn(request);

    assertThat(response).isNotNull();
    assertThat(response.getReturnId()).isEqualTo(36L);
    assertThat(response.getStatus()).isEqualTo("PENDING");
  }

  @Test
  @DisplayName("Tạo yêu cầu đổi trả thành công khi yêu cầu trước đó đã bị bác bỏ (REJECTED)")
  void createReturn_AfterRejected_Success() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);
    when(orderRepository.findById(210L)).thenReturn(Optional.of(testOrder));

    ReturnRequest rejectedReturn =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .reason("[Yêu cầu hoàn trả một phần: iPhone 15 Pro Max (x1)] - Lỗi pin")
            .requestedItems(
                "[{\"variantId\":102,\"productName\":\"iPhone 15 Pro Max\",\"sku\":\"IP15PM-256-TI\",\"quantity\":1,\"unitPrice\":28990000}]")
            .status("REJECTED")
            .rejectReason("Chưa cung cấp đủ video mở hộp")
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();

    when(returnRequestRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(anyCollection()))
        .thenReturn(List.of(rejectedReturn));

    ReturnRequest newReturn =
        ReturnRequest.builder()
            .returnId(37L)
            .order(testOrder)
            .requestedBy(testUser)
            .reason(
                "[Yêu cầu hoàn trả một phần: iPhone 15 Pro Max (x1)] - Lỗi pin (đã bổ sung video)")
            .requestedItems(
                "[{\"variantId\":102,\"productName\":\"iPhone 15 Pro Max\",\"sku\":\"IP15PM-256-TI\",\"quantity\":1,\"unitPrice\":28990000}]")
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();

    when(returnRequestRepository.save(any(ReturnRequest.class))).thenReturn(newReturn);

    CreateReturnRequest request =
        CreateReturnRequest.builder()
            .orderId(210L)
            .reason(
                "[Yêu cầu hoàn trả một phần: iPhone 15 Pro Max (x1)] - Lỗi pin (đã bổ sung video)")
            .evidenceImages(List.of("https://phoneshop.vn/uploads/pin2.jpg"))
            .items(
                List.of(
                    com.wedbackendservice.nextgen.module.order.dto.client.request.ReturnItemRequest
                        .builder()
                        .variantId(102L)
                        .productName("iPhone 15 Pro Max")
                        .sku("IP15PM-256-TI")
                        .quantity(1)
                        .unitPrice(BigDecimal.valueOf(28990000))
                        .build()))
            .build();

    CreateReturnResponse response = returnsService.createReturn(request);

    assertThat(response).isNotNull();
    assertThat(response.getReturnId()).isEqualTo(37L);
    assertThat(response.getStatus()).isEqualTo("PENDING");
  }

  @Test
  @DisplayName(
      "Thẩm định Chấp thuận (APPROVED): Cập nhật trạng thái, Hoàn kho MB04 và Kích hoạt hoàn tiền")
  void inspectReturn_Approved_With_MB04_And_Refund() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

    ReturnRequest pendingReturn =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .status("PENDING")
            .build();

    when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(pendingReturn));

    ProductVariant variant = ProductVariant.builder().variantId(101L).sku("IP15PM-256-TI").build();
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variant));

    OrderItem orderItem =
        OrderItem.builder().orderItemId(1L).order(testOrder).variant(variant).quantity(1).build();
    when(orderItemRepository.findByOrderId(210L)).thenReturn(List.of(orderItem));

    Inventory inventory =
        Inventory.builder()
            .inventoryId(1L)
            .variant(variant)
            .quantity(8)
            .reservedQuantity(5)
            .minThreshold(5)
            .build();
    when(inventoryRepository.findWithLockByVariantId(101L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.increaseQuantityAtomic(eq(101L), eq(1), any(LocalDateTime.class)))
        .thenReturn(1);

    InspectReturnRequest request =
        InspectReturnRequest.builder()
            .status("APPROVED")
            .inspectionResult("Xác nhận lỗi panel")
            .refundAmount(BigDecimal.valueOf(29990000))
            .restockItems(List.of(RestockItemRequest.builder().variantId(101L).quantity(1).build()))
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
    assertThat(response.getInventoryRestock().getRestockedItems().get(0).getNewOnHandQty())
        .isEqualTo(9);
    assertThat(response.getInventoryRestock().getRestockedItems().get(0).getNewAvailableQty())
        .isEqualTo(4);

    verify(returnRequestRepository, times(1)).saveAndFlush(pendingReturn);
    verify(inventoryRepository, times(1))
        .increaseQuantityAtomic(eq(101L), eq(1), any(LocalDateTime.class));
    verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
    verify(paymentRepository, times(1)).save(any(Payment.class));
    verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
    verify(notificationRepository, times(1)).save(any(Notification.class));
  }

  @Test
  @DisplayName("Thẩm định Chấp thuận thất bại khi số tiền hoàn vượt quá giá trị đơn hàng")
  void inspectReturn_RefundExceedsOrderFinalAmount() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

    ReturnRequest pendingReturn =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .status("PENDING")
            .build();
    when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(pendingReturn));

    InspectReturnRequest request =
        InspectReturnRequest.builder()
            .status("APPROVED")
            .inspectionResult("Lỗi nhà sản xuất")
            .refundAmount(BigDecimal.valueOf(50000000)) // exceeds testOrder finalAmount 29990000
            .restockItems(List.of(RestockItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    assertThatThrownBy(() -> returnsService.inspectReturn(35L, request))
        .isInstanceOf(AppException.class)
        .satisfies(
            e ->
                assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
  }

  @Test
  @DisplayName(
      "Thẩm định Chấp thuận thất bại khi số tiền hoàn vượt quá số tiền còn lại sau lần hoàn trước")
  void inspectReturn_RefundExceedsCumulativeRemaining_ThrowsException() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

    // Đơn hàng 29.990.000đ, đã hoàn 20.000.000đ ở phiếu khác (returnId 34)
    ReturnRequest previousApprovedReturn =
        ReturnRequest.builder()
            .returnId(34L)
            .order(testOrder)
            .status("APPROVED")
            .refundAmount(BigDecimal.valueOf(20000000))
            .build();
    when(returnRequestRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(List.of(210L)))
        .thenReturn(List.of(previousApprovedReturn));

    ReturnRequest pendingReturn =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .status("PENDING")
            .build();
    when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(pendingReturn));

    // Yêu cầu hoàn thêm 15.000.000đ (20tr + 15tr = 35tr > 29.99tr)
    InspectReturnRequest request =
        InspectReturnRequest.builder()
            .status("APPROVED")
            .inspectionResult("Lỗi camera")
            .refundAmount(BigDecimal.valueOf(15000000))
            .restockItems(List.of(RestockItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    assertThatThrownBy(() -> returnsService.inspectReturn(35L, request))
        .isInstanceOf(AppException.class)
        .satisfies(
            e ->
                assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
  }

  @Test
  @DisplayName("Thẩm định Chấp thuận hoàn toàn bộ đơn có voucher thì không hoàn lại lượt dùng voucher")
  void inspectReturn_FullRefund_DoesNotDecrementVoucherCount() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

    Voucher testVoucher =
        Voucher.builder().voucherId(99L).voucherCode("GIAM200K").usedCount(5).build();
    testOrder.setVoucher(testVoucher);

    ReturnRequest pendingReturn =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .status("PENDING")
            .build();
    when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(pendingReturn));
    when(returnRequestRepository.findByOrder_OrderIdInOrderByCreatedAtDesc(List.of(210L)))
        .thenReturn(List.of(pendingReturn));

    ProductVariant variant = ProductVariant.builder().variantId(101L).sku("IP15PM").build();
    OrderItem orderItem = OrderItem.builder().variant(variant).quantity(1).build();
    when(orderItemRepository.findByOrderId(210L)).thenReturn(List.of(orderItem));
    when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variant));

    Inventory inv = Inventory.builder().quantity(10).reservedQuantity(0).build();
    when(inventoryRepository.findWithLockByVariantId(101L)).thenReturn(Optional.of(inv));
    when(inventoryRepository.increaseQuantityAtomic(eq(101L), eq(1), any(LocalDateTime.class)))
        .thenReturn(1);

    InspectReturnRequest request =
        InspectReturnRequest.builder()
            .status("APPROVED")
            .inspectionResult("Lỗi màn hình hoàn toàn bộ")
            .refundAmount(BigDecimal.valueOf(29990000)) // Hoàn 100%
            .restockItems(List.of(RestockItemRequest.builder().variantId(101L).quantity(1).build()))
            .build();

    InspectReturnResponse response = returnsService.inspectReturn(35L, request);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo("APPROVED");
    verify(voucherRepository, never()).decrementUsedCountAtomic(anyLong());
  }

  @Test
  @DisplayName("Thẩm định Chấp thuận thất bại khi sản phẩm hoàn kho không có trong đơn hàng")
  void inspectReturn_VariantNotInOrder() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

    ReturnRequest pendingReturn =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .status("PENDING")
            .build();
    when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(pendingReturn));

    ProductVariant variant101 = ProductVariant.builder().variantId(101L).build();
    OrderItem orderItem =
        OrderItem.builder()
            .orderItemId(1L)
            .order(testOrder)
            .variant(variant101)
            .quantity(1)
            .build();
    when(orderItemRepository.findByOrderId(210L)).thenReturn(List.of(orderItem));

    InspectReturnRequest request =
        InspectReturnRequest.builder()
            .status("APPROVED")
            .inspectionResult("Lỗi")
            .refundAmount(BigDecimal.valueOf(1000000))
            .restockItems(List.of(RestockItemRequest.builder().variantId(999L).quantity(1).build()))
            .build();

    assertThatThrownBy(() -> returnsService.inspectReturn(35L, request))
        .isInstanceOf(AppException.class)
        .satisfies(
            e ->
                assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
  }

  @Test
  @DisplayName("Thẩm định Từ chối (REJECTED) không tác động kho")
  void inspectReturn_Rejected() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

    ReturnRequest pendingReturn =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .status("PENDING")
            .build();

    when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(pendingReturn));

    InspectReturnRequest request =
        InspectReturnRequest.builder()
            .status("REJECTED")
            .inspectionResult("Máy có dấu hiệu rơi vỡ, nứt kính lưng và cấn móp nặng góc máy")
            .rejectReason("Lỗi do người sử dụng làm rơi vỡ, từ chối chính sách bảo hành/đổi trả.")
            .build();

    InspectReturnResponse response = returnsService.inspectReturn(35L, request);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo("REJECTED");
    assertThat(response.getRejectReason()).contains("rơi vỡ");
    assertThat(response.getInventoryRestocked()).isFalse();

    verifyNoInteractions(inventoryRepository);
    verifyNoInteractions(paymentRepository);
    verify(returnRequestRepository, times(1)).saveAndFlush(pendingReturn);
    verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
    verify(notificationRepository, times(1)).save(any(Notification.class));
    verify(auditLogRepository, times(1))
        .log(eq(staffUser), eq("RETURN_REJECTED"), anyString(), eq(35L), anyString(), anyString());
  }

  @Test
  @DisplayName("Không được phép thẩm định lại yêu cầu đổi trả đã kết luận")
  void inspectReturn_AlreadyProcessed() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

    ReturnRequest approvedReturn = ReturnRequest.builder().returnId(35L).status("APPROVED").build();

    when(returnRequestRepository.findById(35L)).thenReturn(Optional.of(approvedReturn));

    InspectReturnRequest request =
        InspectReturnRequest.builder()
            .status("REJECTED")
            .inspectionResult("Từ chối lại")
            .rejectReason("Lý do khác")
            .build();

    assertThatThrownBy(() -> returnsService.inspectReturn(35L, request))
        .isInstanceOf(AppException.class)
        .satisfies(
            e ->
                assertThat(((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RETURN_ALREADY_PROCESSED));
  }

  @Test
  @DisplayName("Lấy danh sách yêu cầu đổi trả phân trang và thống kê")
  void getAdminReturns_Success() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);

    ReturnRequest ret =
        ReturnRequest.builder()
            .returnId(35L)
            .order(testOrder)
            .requestedBy(testUser)
            .reason("Màn hình sọc")
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();

    when(returnRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(ret)));
    when(returnRequestRepository.countByStatusGrouped())
        .thenReturn(
            List.of(
                new Object[] {"PENDING", 3L},
                new Object[] {"INSPECTING", 2L},
                new Object[] {"APPROVED", 42L},
                new Object[] {"REJECTED", 8L}));

    AdminReturnListResponse response =
        returnsService.getAdminReturns(1, 20, "PENDING", null, "createdAt", "desc");

    assertThat(response).isNotNull();
    assertThat(response.getItems()).hasSize(1);
    assertThat(response.getSummary().getTotalPending()).isEqualTo(5L);
    assertThat(response.getSummary().getTotalApproved()).isEqualTo(42L);
    assertThat(response.getSummary().getTotalRejected()).isEqualTo(8L);
    assertThat(response.getPagination().getTotalItems()).isEqualTo(1L);
  }
}
