package com.wedbackendservice.nextgen.module.cart.service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.module.cart.dto.client.request.CartItemAddRequest;
import com.wedbackendservice.nextgen.module.cart.dto.client.response.CartItemAddResponse;
import com.wedbackendservice.nextgen.module.cart.dto.client.response.CartItemDeleteResponse;
import com.wedbackendservice.nextgen.module.cart.entity.Cart;
import com.wedbackendservice.nextgen.module.cart.entity.CartItem;
import com.wedbackendservice.nextgen.module.cart.repository.CartItemRepository;
import com.wedbackendservice.nextgen.module.cart.repository.CartRepository;
import com.wedbackendservice.nextgen.module.inventory.entity.Inventory;
import com.wedbackendservice.nextgen.module.inventory.repository.InventoryRepository;
import com.wedbackendservice.nextgen.module.product.entity.Product;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
import com.wedbackendservice.nextgen.module.product.repository.ProductImageRepository;
import com.wedbackendservice.nextgen.module.product.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.voucher.repository.PromotionVariantRepository;
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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

  @Mock private SecurityHelper securityHelper;

  @Mock private CartRepository cartRepository;

  @Mock private CartItemRepository cartItemRepository;

  @Mock private ProductVariantRepository productVariantRepository;

  @Mock private InventoryRepository inventoryRepository;

  @Mock private PromotionVariantRepository promotionVariantRepository;

  @Mock private ProductImageRepository productImageRepository;

  @InjectMocks private CartService cartService;

  private User testUser;
  private Product testProduct;
  private ProductVariant testVariant;
  private Cart testCart;
  private Inventory testInventory;

  @BeforeEach
  void setUp() {
    testUser = User.builder().userId(1L).role(Role.CUSTOMER).build();

    testProduct =
        Product.builder().productId(10L).productName("MacBook Pro").status("ACTIVE").build();

    testVariant =
        ProductVariant.builder()
            .variantId(100L)
            .product(testProduct)
            .sku("MBP-M3-16-512")
            .status("ACTIVE")
            .price(BigDecimal.valueOf(35000000))
            .build();

    testCart =
        Cart.builder()
            .cartId(200L)
            .user(testUser)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    testInventory =
        Inventory.builder()
            .inventoryId(1L)
            .variant(testVariant)
            .quantity(10)
            .reservedQuantity(2)
            .build();
  }

  @Test
  @DisplayName("addItemToCart successfully adds new item when stock is sufficient")
  void addItemToCart_success() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);
    when(productVariantRepository.findById(100L)).thenReturn(Optional.of(testVariant));
    when(inventoryRepository.findByVariant_VariantId(100L)).thenReturn(Optional.of(testInventory));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
    when(cartItemRepository.findByCartIdAndVariantId(200L, 100L)).thenReturn(Optional.empty());

    CartItem savedCartItem =
        CartItem.builder().cartItemId(50L).cart(testCart).variant(testVariant).quantity(2).build();
    when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedCartItem);

    CartItemAddRequest request = CartItemAddRequest.builder().variantId(100L).quantity(2).build();
    CartItemAddResponse response = cartService.addItemToCart(request);

    assertThat(response).isNotNull();
    assertThat(response.getCartItemId()).isEqualTo(50L);
    assertThat(response.getQuantity()).isEqualTo(2);
    verify(cartItemRepository).save(any(CartItem.class));
  }

  @Test
  @DisplayName("addItemToCart throws exception when requested quantity exceeds available stock")
  void addItemToCart_insufficientStock_throwsException() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);
    when(productVariantRepository.findById(100L)).thenReturn(Optional.of(testVariant));
    when(inventoryRepository.findByVariant_VariantId(100L))
        .thenReturn(Optional.of(testInventory)); // available = 10 - 2 = 8
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
    when(cartItemRepository.findByCartIdAndVariantId(200L, 100L)).thenReturn(Optional.empty());

    CartItemAddRequest request = CartItemAddRequest.builder().variantId(100L).quantity(10).build();

    assertThatThrownBy(() -> cartService.addItemToCart(request))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("không đủ số lượng yêu cầu");
  }

  @Test
  @DisplayName("removeItemFromCart deletes item and returns remaining count")
  void removeItemFromCart_success() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);

    CartItem cartItem =
        CartItem.builder().cartItemId(50L).cart(testCart).variant(testVariant).quantity(1).build();
    when(cartItemRepository.findById(50L)).thenReturn(Optional.of(cartItem));
    when(cartItemRepository.findByCartId(200L)).thenReturn(List.of());

    CartItemDeleteResponse response = cartService.removeItemFromCart(50L);

    assertThat(response).isNotNull();
    assertThat(response.getDeletedItemId()).isEqualTo(50L);
    assertThat(response.getRemainingItems()).isEqualTo(0);
    verify(cartItemRepository).delete(cartItem);
  }
}
