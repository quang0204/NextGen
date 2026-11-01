package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.security.SecurityHelper;

import com.wedbackendservice.nextgen.dto.response.cart.*;
import com.wedbackendservice.nextgen.dto.request.cart.CartItemAddRequest;
import com.wedbackendservice.nextgen.dto.request.cart.CartItemUpdateRequest;
import com.wedbackendservice.nextgen.dto.request.cart.CartSyncRequest;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final SecurityHelper securityHelper;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final PromotionVariantRepository promotionVariantRepository;
    private final ProductImageRepository productImageRepository;

    /**
     * API 7.1: Lấy thông tin giỏ hàng của người dùng (Authenticated)
     */
    @Transactional
    public CartResponse getCart(String authHeader) {
        User user = securityHelper.authenticate(authHeader);
        Cart cart = getOrCreateCart(user);

        List<CartItem> items = cartItemRepository.findByCartId(cart.getCartId());
        List<CartItemResponse> itemResponses = new ArrayList<>();

        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            Product product = variant != null ? variant.getProduct() : null;

            int availableStock = variant != null ? getAvailableStock(variant.getVariantId()) : 0;
            boolean inStock = availableStock >= item.getQuantity();

            PriceInfo priceInfo = resolvePrice(variant);
            BigDecimal lineTotal = priceInfo.currentPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(lineTotal);
            totalQuantity += item.getQuantity();

            CartItemResponse resp = CartItemResponse.builder()
                    .cartItemId(item.getCartItemId())
                    .variantId(variant != null ? variant.getVariantId() : null)
                    .productId(product != null ? product.getProductId() : null)
                    .productName(product != null ? product.getProductName() : null)
                    .sku(variant != null ? variant.getSku() : null)
                    .color(variant != null ? variant.getColor() : null)
                    .ramGb(variant != null ? variant.getRamGb() : null)
                    .romGb(variant != null ? variant.getRomGb() : null)
                    .imageUrl(resolveImageUrl(variant))
                    .listPrice(priceInfo.listPrice)
                    .currentPrice(priceInfo.currentPrice)
                    .isFlashSale(priceInfo.isFlashSale)
                    .quantity(item.getQuantity())
                    .lineTotal(lineTotal)
                    .availableStock(availableStock)
                    .inStock(inStock)
                    .addedAt(item.getCreatedAt())
                    .build();

            itemResponses.add(resp);
        }

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .userId(user.getUserId())
                .items(itemResponses)
                .totalQuantity(totalQuantity)
                .subtotal(subtotal)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    /**
     * API 7.2: Thêm sản phẩm biến thể (SKU) vào giỏ hàng (Authenticated)
     */
    @Transactional
    public CartItemAddResponse addItemToCart(String authHeader, CartItemAddRequest request) {
        User user = securityHelper.authenticate(authHeader);

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));

        if ("INACTIVE".equalsIgnoreCase(variant.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Sản phẩm hiện không kinh doanh hoặc tạm ngừng.");
        }

        int requestedQty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
        int availableStock = getAvailableStock(variant.getVariantId());

        if (requestedQty > availableStock) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Sản phẩm hiện chỉ còn " + availableStock + " máy, không đủ số lượng yêu cầu");
        }

        Cart cart = getOrCreateCart(user);
        Optional<CartItem> existingOpt = cartItemRepository.findByCartIdAndVariantId(cart.getCartId(), variant.getVariantId());

        CartItem savedItem;
        if (existingOpt.isPresent()) {
            CartItem existing = existingOpt.get();
            int newQuantity = existing.getQuantity() + requestedQty;
            if (newQuantity > availableStock) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Sản phẩm hiện chỉ còn " + availableStock + " máy, không đủ số lượng yêu cầu");
            }
            existing.setQuantity(newQuantity);
            existing.setUpdatedAt(LocalDateTime.now());
            savedItem = cartItemRepository.save(existing);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(requestedQty)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            savedItem = cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        List<CartItem> allItems = cartItemRepository.findByCartId(cart.getCartId());
        int totalCartItems = allItems.stream().mapToInt(CartItem::getQuantity).sum();

        return CartItemAddResponse.builder()
                .cartItemId(savedItem.getCartItemId())
                .variantId(variant.getVariantId())
                .quantity(savedItem.getQuantity())
                .totalCartItems(totalCartItems)
                .build();
    }

    /**
     * API 7.3: Thay đổi số lượng mua (+ / - / nhập số) (Authenticated)
     */
    @Transactional
    public CartItemUpdateResponse updateItemQuantity(String authHeader, Long itemId, CartItemUpdateRequest request) {
        User user = securityHelper.authenticate(authHeader);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Cố tình sửa dòng giỏ hàng của tài khoản khác.");
        }

        Cart cart = item.getCart();
        int requestedQty = request.getQuantity() != null ? request.getQuantity() : 0;

        if (requestedQty <= 0) {
            cartItemRepository.delete(item);
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);

            BigDecimal newSubtotal = calculateSubtotal(cart.getCartId());
            return CartItemUpdateResponse.builder()
                    .cartItemId(itemId)
                    .variantId(item.getVariant().getVariantId())
                    .quantity(0)
                    .lineTotal(BigDecimal.ZERO)
                    .newSubtotal(newSubtotal)
                    .build();
        }

        int availableStock = getAvailableStock(item.getVariant().getVariantId());
        if (requestedQty > availableStock) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Kho hàng chỉ còn tối đa " + availableStock + " thiết bị khả dụng.");
        }

        item.setQuantity(requestedQty);
        item.setUpdatedAt(LocalDateTime.now());
        CartItem updated = cartItemRepository.save(item);

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        PriceInfo priceInfo = resolvePrice(item.getVariant());
        BigDecimal lineTotal = priceInfo.currentPrice.multiply(BigDecimal.valueOf(updated.getQuantity()));
        BigDecimal newSubtotal = calculateSubtotal(cart.getCartId());

        return CartItemUpdateResponse.builder()
                .cartItemId(itemId)
                .variantId(item.getVariant().getVariantId())
                .quantity(updated.getQuantity())
                .lineTotal(lineTotal)
                .newSubtotal(newSubtotal)
                .build();
    }

    /**
     * API 7.4: Xóa 1 biến thể SKU khỏi giỏ hàng (Authenticated)
     */
    @Transactional
    public CartItemDeleteResponse removeItemFromCart(String authHeader, Long itemId) {
        User user = securityHelper.authenticate(authHeader);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        Cart cart = item.getCart();
        cartItemRepository.delete(item);

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        List<CartItem> remaining = cartItemRepository.findByCartId(cart.getCartId());
        BigDecimal newSubtotal = calculateSubtotal(cart.getCartId());

        return CartItemDeleteResponse.builder()
                .deletedItemId(itemId)
                .remainingItems(remaining.size())
                .newSubtotal(newSubtotal)
                .build();
    }

    /**
     * API 7.5: Xóa sạch toàn bộ giỏ hàng (Authenticated)
     */
    @Transactional
    public void clearCart(String authHeader) {
        User user = securityHelper.authenticate(authHeader);

        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getUserId());
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cartItemRepository.deleteAllByCartId(cart.getCartId());
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);
        }
    }

    /**
     * API 7.6: Đồng bộ giỏ hàng vãng lai (Guest) vào tài khoản khi Đăng nhập (Authenticated)
     */
    @Transactional
    public CartSyncResponse syncCart(String authHeader, CartSyncRequest request) {
        User user = securityHelper.authenticate(authHeader);
        Cart cart = getOrCreateCart(user);

        int syncedCount = 0;
        if (request != null && request.getGuestItems() != null) {
            for (CartItemAddRequest guestItem : request.getGuestItems()) {
                if (guestItem.getVariantId() == null || guestItem.getQuantity() == null || guestItem.getQuantity() <= 0) {
                    continue;
                }

                Optional<ProductVariant> varOpt = productVariantRepository.findById(guestItem.getVariantId());
                if (varOpt.isEmpty() || "INACTIVE".equalsIgnoreCase(varOpt.get().getStatus())) {
                    continue;
                }

                ProductVariant variant = varOpt.get();
                int availableStock = getAvailableStock(variant.getVariantId());
                if (availableStock <= 0) {
                    continue;
                }

                Optional<CartItem> existingOpt = cartItemRepository.findByCartIdAndVariantId(cart.getCartId(), variant.getVariantId());
                if (existingOpt.isPresent()) {
                    CartItem existing = existingOpt.get();
                    int newQty = existing.getQuantity() + guestItem.getQuantity();
                    if (newQty > availableStock) {
                        newQty = availableStock;
                    }
                    existing.setQuantity(newQty);
                    existing.setUpdatedAt(LocalDateTime.now());
                    cartItemRepository.save(existing);
                    syncedCount++;
                } else {
                    int addQty = Math.min(guestItem.getQuantity(), availableStock);
                    if (addQty > 0) {
                        CartItem newItem = CartItem.builder()
                                .cart(cart)
                                .variant(variant)
                                .quantity(addQty)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        cartItemRepository.save(newItem);
                        syncedCount++;
                    }
                }
            }
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        List<CartItem> currentItems = cartItemRepository.findByCartId(cart.getCartId());
        int totalItems = currentItems.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal subtotal = calculateSubtotal(cart.getCartId());

        return CartSyncResponse.builder()
                .cartId(cart.getCartId())
                .totalItems(totalItems)
                .syncedCount(syncedCount)
                .subtotal(subtotal)
                .build();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getUserId())
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .user(user)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    public int getAvailableStock(Long variantId) {
        return inventoryRepository.findByVariant_VariantId(variantId)
                .map(inv -> {
                    int onHand = inv.getQuantity() != null ? inv.getQuantity() : 0;
                    int reserved = inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0;
                    return Math.max(0, onHand - reserved);
                })
                .orElse(0);
    }

    public record PriceInfo(BigDecimal listPrice, BigDecimal currentPrice, boolean isFlashSale) {}

    public PriceInfo resolvePrice(ProductVariant variant) {
        if (variant == null) {
            return new PriceInfo(BigDecimal.ZERO, BigDecimal.ZERO, false);
        }

        BigDecimal listPrice = variant.getPrice() != null ? variant.getPrice() :
                (variant.getProduct() != null && variant.getProduct().getBasePrice() != null
                        ? variant.getProduct().getBasePrice() : BigDecimal.ZERO);

        List<PromotionVariant> promoList = promotionVariantRepository.findActivePromotionsForVariant(
                variant.getVariantId(), LocalDateTime.now());

        if (promoList != null && !promoList.isEmpty() && promoList.get(0).getPromotionalPrice() != null) {
            return new PriceInfo(listPrice, promoList.get(0).getPromotionalPrice(), true);
        }

        return new PriceInfo(listPrice, listPrice, false);
    }

    public String resolveImageUrl(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        if (variant.getImageUrl() != null && !variant.getImageUrl().isBlank()) {
            return variant.getImageUrl();
        }
        if (variant.getProduct() != null) {
            Optional<ProductImage> primaryImg = productImageRepository.findFirstByProduct_ProductIdAndIsPrimaryTrue(
                    variant.getProduct().getProductId());
            if (primaryImg.isPresent() && primaryImg.get().getImageUrl() != null) {
                return primaryImg.get().getImageUrl();
            }
            Optional<ProductImage> firstImg = productImageRepository.findFirstByProduct_ProductIdOrderByDisplayOrderAsc(
                    variant.getProduct().getProductId());
            if (firstImg.isPresent() && firstImg.get().getImageUrl() != null) {
                return firstImg.get().getImageUrl();
            }
            return variant.getProduct().getThumbnailUrl();
        }
        return null;
    }

    public BigDecimal calculateSubtotal(Long cartId) {
        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            PriceInfo pi = resolvePrice(item.getVariant());
            BigDecimal line = pi.currentPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(line);
        }
        return subtotal;
    }
}
