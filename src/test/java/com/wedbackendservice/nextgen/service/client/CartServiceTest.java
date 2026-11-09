package com.wedbackendservice.nextgen.service.client;

import com.wedbackendservice.nextgen.dto.client.request.cart.CartItemAddRequest;
import com.wedbackendservice.nextgen.dto.client.response.cart.CartItemAddResponse;
import com.wedbackendservice.nextgen.dto.client.response.cart.CartItemDeleteResponse;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private PromotionVariantRepository promotionVariantRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Product testProduct;
    private ProductVariant testVariant;
    private Cart testCart;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testUser = User.builder().userId(1L).role(Role.CUSTOMER).build();

        testProduct = Product.builder().productId(10L).productName("MacBook Pro").status("ACTIVE").build();

        testVariant = ProductVariant.builder()
                .variantId(100L)
                .product(testProduct)
                .sku("MBP-M3-16-512")
                .status("ACTIVE")
                .price(BigDecimal.valueOf(35000000))
                .build();

        testCart = Cart.builder()
                .cartId(200L)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testInventory = Inventory.builder()
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

        CartItem savedCartItem = CartItem.builder()
                .cartItemId(50L)
                .cart(testCart)
                .variant(testVariant)
                .quantity(2)
                .build();
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedCartItem);
        when(cartItemRepository.findByCartId(200L)).thenReturn(List.of(savedCartItem));

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
        when(inventoryRepository.findByVariant_VariantId(100L)).thenReturn(Optional.of(testInventory)); // available = 10 - 2 = 8
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

        CartItem cartItem = CartItem.builder()
                .cartItemId(50L)
                .cart(testCart)
                .variant(testVariant)
                .quantity(1)
                .build();
        when(cartItemRepository.findById(50L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.findByCartId(200L)).thenReturn(List.of());

        CartItemDeleteResponse response = cartService.removeItemFromCart(50L);

        assertThat(response).isNotNull();
        assertThat(response.getDeletedItemId()).isEqualTo(50L);
        assertThat(response.getRemainingItems()).isEqualTo(0);
        verify(cartItemRepository).delete(cartItem);
    }
}
