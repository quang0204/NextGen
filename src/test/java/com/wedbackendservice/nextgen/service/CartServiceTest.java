package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.request.cart.CartItemAddRequest;
import com.wedbackendservice.nextgen.dto.request.cart.CartItemUpdateRequest;
import com.wedbackendservice.nextgen.dto.response.cart.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

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

    private User user;
    private Cart cart;
    private Product product;
    private ProductVariant variant;
    private CartItem cartItem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(101L)
                .fullName("Nguyễn Văn Hoàng")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        cart = Cart.builder()
                .cartId(1L)
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        product = Product.builder()
                .productId(10L)
                .productName("iPhone 15 Pro")
                .basePrice(new BigDecimal("28000000"))
                .build();

        variant = ProductVariant.builder()
                .variantId(100L)
                .product(product)
                .sku("IPHONE15P-128-TI")
                .price(new BigDecimal("28990000"))
                .status("ACTIVE")
                .build();

        cartItem = CartItem.builder()
                .cartItemId(1L)
                .cart(cart)
                .variant(variant)
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        inventory = Inventory.builder()
                .inventoryId(1L)
                .variant(variant)
                .quantity(10)
                .reservedQuantity(2)
                .build();
    }

    @Test
    void testGetCart_Success() {
        when(securityHelper.authenticate("Bearer token")).thenReturn(user);
        when(cartRepository.findByUserId(101L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByVariant_VariantId(100L)).thenReturn(Optional.of(inventory));
        when(promotionVariantRepository.findActivePromotionsForVariant(eq(100L), any())).thenReturn(List.of());
        when(productImageRepository.findFirstByProduct_ProductIdAndIsPrimaryTrue(10L)).thenReturn(Optional.empty());

        CartResponse resp = cartService.getCart("Bearer token");

        assertNotNull(resp);
        assertEquals(1L, resp.getCartId());
        assertEquals(1, resp.getItems().size());
        assertEquals(2, resp.getTotalQuantity());
        assertEquals(new BigDecimal("57980000"), resp.getSubtotal());
    }

    @Test
    void testAddItemToCart_Success() {
        when(securityHelper.authenticate("Bearer token")).thenReturn(user);
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByVariant_VariantId(100L)).thenReturn(Optional.of(inventory));
        when(cartRepository.findByUserId(101L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndVariantId(1L, 100L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));

        CartItemAddRequest req = CartItemAddRequest.builder()
                .variantId(100L)
                .quantity(2)
                .build();

        CartItemAddResponse resp = cartService.addItemToCart("Bearer token", req);

        assertNotNull(resp);
        assertEquals(1L, resp.getCartItemId());
        assertEquals(100L, resp.getVariantId());
        assertEquals(2, resp.getQuantity());
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    void testAddItemToCart_InsufficientStock_ThrowsException() {
        when(securityHelper.authenticate("Bearer token")).thenReturn(user);
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(variant));
        // Available stock is 10 - 2 = 8
        when(inventoryRepository.findByVariant_VariantId(100L)).thenReturn(Optional.of(inventory));

        CartItemAddRequest req = CartItemAddRequest.builder()
                .variantId(100L)
                .quantity(20)
                .build();

        AppException ex = assertThrows(AppException.class, () -> cartService.addItemToCart("Bearer token", req));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
    }

    @Test
    void testUpdateItemQuantity_Success() {
        when(securityHelper.authenticate("Bearer token")).thenReturn(user);
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(inventoryRepository.findByVariant_VariantId(100L)).thenReturn(Optional.of(inventory));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(promotionVariantRepository.findActivePromotionsForVariant(eq(100L), any())).thenReturn(List.of());
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));

        CartItemUpdateRequest req = CartItemUpdateRequest.builder()
                .quantity(3)
                .build();

        CartItemUpdateResponse resp = cartService.updateItemQuantity("Bearer token", 1L, req);

        assertNotNull(resp);
        assertEquals(1L, resp.getCartItemId());
        verify(cartItemRepository, times(1)).save(cartItem);
    }

    @Test
    void testRemoveItemFromCart_Success() {
        when(securityHelper.authenticate("Bearer token")).thenReturn(user);
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        CartItemDeleteResponse resp = cartService.removeItemFromCart("Bearer token", 1L);

        assertNotNull(resp);
        assertEquals(1L, resp.getDeletedItemId());
        verify(cartItemRepository, times(1)).delete(cartItem);
    }
}
