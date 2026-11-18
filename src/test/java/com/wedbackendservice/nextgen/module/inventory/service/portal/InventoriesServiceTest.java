package com.wedbackendservice.nextgen.module.inventory.service.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.module.inventory.dto.portal.request.InventoryAdjustmentRequest;
import com.wedbackendservice.nextgen.module.inventory.dto.portal.response.InventoryAdjustmentResponse;
import com.wedbackendservice.nextgen.module.inventory.entity.Inventory;
import com.wedbackendservice.nextgen.module.inventory.entity.InventoryTransaction;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryTransactionRepository;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.product.entity.Product;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.product.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoriesServiceTest {

  @Mock private SecurityHelper securityHelper;

  @Mock private InventoryRepository inventoryRepository;

  @Mock private InventoryTransactionRepository inventoryTransactionRepository;

  @Mock private ProductVariantRepository productVariantRepository;

  @Mock private AuditLogRepository auditLogRepository;

  @InjectMocks private InventoriesService inventoriesService;

  private User staffUser;
  private Product product;
  private ProductVariant variant;
  private Inventory inventory;

  @BeforeEach
  void setUp() {
    staffUser = User.builder().userId(1L).fullName("Thủ Kho Test").role(Role.STAFF).build();

    product = Product.builder().productId(10L).productName("iPhone 15 Pro Max").build();

    variant =
        ProductVariant.builder()
            .variantId(100L)
            .product(product)
            .sku("IP15PM-256-NAT")
            .price(BigDecimal.valueOf(30000000))
            .build();

    inventory =
        Inventory.builder()
            .inventoryId(50L)
            .variant(variant)
            .quantity(20)
            .reservedQuantity(5)
            .minThreshold(5)
            .build();
  }

  @Test
  void adjustInventory_Success() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(productVariantRepository.findById(100L)).thenReturn(Optional.of(variant));
    when(inventoryRepository.findByVariant_VariantId(100L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
    when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
        .thenAnswer(
            inv -> {
              InventoryTransaction t = inv.getArgument(0);
              t.setTxnId(999L);
              return t;
            });

    InventoryAdjustmentRequest req =
        InventoryAdjustmentRequest.builder()
            .variantId(100L)
            .actualCount(25)
            .reason("Kiểm kê thực tế dư 5 máy")
            .build();

    InventoryAdjustmentResponse res = inventoriesService.adjustInventory(req);

    assertThat(res).isNotNull();
    assertThat(res.getVariantId()).isEqualTo(100L);
    assertThat(res.getQtyBefore()).isEqualTo(20);
    assertThat(res.getQtyActual()).isEqualTo(25);
    assertThat(res.getQuantityChange()).isEqualTo(5);
    assertThat(res.getAvailableQty()).isEqualTo(20);

    verify(inventoryRepository).save(any(Inventory.class));
    verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    verify(auditLogRepository)
        .log(
            eq(staffUser),
            eq("INVENTORY_ADJUSTMENT"),
            eq("Inventories"),
            eq(50L),
            anyString(),
            anyString());
  }

  @Test
  void adjustInventory_ActualCountLessThanReserved_ThrowsException() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(productVariantRepository.findById(100L)).thenReturn(Optional.of(variant));
    when(inventoryRepository.findByVariant_VariantId(100L)).thenReturn(Optional.of(inventory));

    InventoryAdjustmentRequest req =
        InventoryAdjustmentRequest.builder()
            .variantId(100L)
            .actualCount(3)
            .reason("Kiểm kê thực tế")
            .build();

    assertThatThrownBy(() -> inventoriesService.adjustInventory(req))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ERR_STOCK_ADJUST_INVALID);

    verify(inventoryRepository, never()).setQuantityAtomic(anyLong(), anyInt(), any());
    verify(inventoryTransactionRepository, never()).save(any());
  }

  @Test
  void adjustInventory_VariantNotFound_ThrowsException() {
    when(securityHelper.authenticateStaffOrAdmin()).thenReturn(staffUser);
    when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

    InventoryAdjustmentRequest req =
        InventoryAdjustmentRequest.builder()
            .variantId(999L)
            .actualCount(10)
            .reason("Kiểm kê")
            .build();

    assertThatThrownBy(() -> inventoriesService.adjustInventory(req))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ERR_VARIANT_NOT_FOUND);
  }
}
