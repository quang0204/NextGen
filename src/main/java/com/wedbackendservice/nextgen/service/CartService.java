package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.request.cart.CartItemAddRequest;
import com.wedbackendservice.nextgen.dto.request.cart.CartItemUpdateRequest;
import com.wedbackendservice.nextgen.dto.request.cart.CartSyncRequest;
import com.wedbackendservice.nextgen.dto.response.cart.*;
import com.wedbackendservice.nextgen.entity.*;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
     * Đọc thuần túy, không ghi tạo mới giỏ hàng trong request GET, giải quyết N+1 bằng Batch Query.
     */
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        User user = securityHelper.getCurrentUser();
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getUserId());

        if (cartOpt.isEmpty()) {
            return CartResponse.builder()
                    .cartId(null)
                    .userId(user.getUserId())
                    .items(List.of())
                    .totalQuantity(0)
                    .subtotal(BigDecimal.ZERO)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        Cart cart = cartOpt.get();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getCartId());

        if (items.isEmpty()) {
            return CartResponse.builder()
                    .cartId(cart.getCartId())
                    .userId(user.getUserId())
                    .items(List.of())
                    .totalQuantity(0)
                    .subtotal(BigDecimal.ZERO)
                    .updatedAt(cart.getUpdatedAt())
                    .build();
        }

        // Batch Query để tránh N+1
        List<Long> variantIds = items.stream()
                .map(item -> item.getVariant() != null ? item.getVariant().getVariantId() : null)
                .filter(Objects::nonNull)
                .toList();

        List<Long> productIds = items.stream()
                .map(item -> (item.getVariant() != null && item.getVariant().getProduct() != null)
                        ? item.getVariant().getProduct().getProductId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Inventory> inventoryMap = inventoryRepository.findByVariant_VariantIdIn(variantIds).stream()
                .collect(Collectors.toMap(inv -> inv.getVariant().getVariantId(), inv -> inv, (a, b) -> a));

        Map<Long, PromotionVariant> promoMap = promotionVariantRepository.findActivePromotionsForVariantIds(variantIds, LocalDateTime.now()).stream()
                .collect(Collectors.toMap(pv -> pv.getVariant().getVariantId(), pv -> pv, (a, b) -> a));

        Map<Long, String> imageMap = productImageRepository.findByProduct_ProductIdInAndIsPrimaryTrue(productIds).stream()
                .collect(Collectors.toMap(img -> img.getProduct().getProductId(), ProductImage::getImageUrl, (a, b) -> a));

        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            Product product = variant != null ? variant.getProduct() : null;

            int availableStock = 0;
            if (variant != null && inventoryMap.containsKey(variant.getVariantId())) {
                Inventory inv = inventoryMap.get(variant.getVariantId());
                int onHand = inv.getQuantity() != null ? inv.getQuantity() : 0;
                int reserved = inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0;
                availableStock = Math.max(0, onHand - reserved);
            }

            boolean inStock = availableStock >= item.getQuantity();
            PriceInfo priceInfo = resolvePriceWithMap(variant, promoMap);
            BigDecimal lineTotal = priceInfo.currentPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(lineTotal);
            totalQuantity += item.getQuantity();

            String imageUrl = null;
            if (variant != null && variant.getImageUrl() != null && !variant.getImageUrl().isBlank()) {
                imageUrl = variant.getImageUrl();
            } else if (product != null && imageMap.containsKey(product.getProductId())) {
                imageUrl = imageMap.get(product.getProductId());
            }

            CartItemResponse resp = CartItemResponse.builder()
                    .cartItemId(item.getCartItemId())
                    .variantId(variant != null ? variant.getVariantId() : null)
                    .productId(product != null ? product.getProductId() : null)
                    .productName(product != null ? product.getProductName() : "Sản phẩm")
                    .sku(variant != null ? variant.getSku() : "")
                    .color(variant != null ? variant.getColor() : "")
                    .ramGb(variant != null ? variant.getRamGb() : null)
                    .romGb(variant != null ? variant.getRomGb() : null)
                    .imageUrl(imageUrl)
                    .listPrice(priceInfo.listPrice)
                    .currentPrice(priceInfo.currentPrice)
                    .isFlashSale(priceInfo.isFlashSale)
                    .quantity(item.getQuantity())
                    .availableStock(availableStock)
                    .lineTotal(lineTotal)
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
    public CartItemAddResponse addItemToCart( CartItemAddRequest request) {
        User user = securityHelper.getCurrentUser();

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));

        if ("INACTIVE".equalsIgnoreCase(variant.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Sản phẩm hiện không kinh doanh hoặc tạm ngừng.");
        }

        if (variant.getProduct() != null && "INACTIVE".equalsIgnoreCase(variant.getProduct().getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_INACTIVE);
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
    public CartItemUpdateResponse updateItemQuantity( Long itemId, CartItemUpdateRequest request) {
        User user = securityHelper.getCurrentUser();

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
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
    public CartItemDeleteResponse removeItemFromCart( Long itemId) {
        User user = securityHelper.getCurrentUser();

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
    public void clearCart() {
        User user = securityHelper.getCurrentUser();

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
     * Đảm bảo tính Idempotent (không cộng dồn lặp lại khi retry), trả về danh sách sản phẩm bị bỏ qua.
     */
    @Transactional
    public CartSyncResponse syncCart(CartSyncRequest request) {
        User user = securityHelper.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        int syncedCount = 0;
        List<CartSyncResponse.SkippedItem> skippedItems = new ArrayList<>();

        if (request != null && request.getGuestItems() != null) {
            for (CartItemAddRequest guestItem : request.getGuestItems()) {
                if (guestItem == null || guestItem.getVariantId() == null || guestItem.getQuantity() == null || guestItem.getQuantity() <= 0) {
                    continue;
                }

                Optional<ProductVariant> varOpt = productVariantRepository.findById(guestItem.getVariantId());
                if (varOpt.isEmpty()) {
                    skippedItems.add(CartSyncResponse.SkippedItem.builder()
                            .variantId(guestItem.getVariantId())
                            .requestedQuantity(guestItem.getQuantity())
                            .reason("Biến thể sản phẩm không tồn tại")
                            .build());
                    continue;
                }

                ProductVariant variant = varOpt.get();
                if ("INACTIVE".equalsIgnoreCase(variant.getStatus())
                        || (variant.getProduct() != null && "INACTIVE".equalsIgnoreCase(variant.getProduct().getStatus()))) {
                    skippedItems.add(CartSyncResponse.SkippedItem.builder()
                            .variantId(variant.getVariantId())
                            .requestedQuantity(guestItem.getQuantity())
                            .reason("Sản phẩm đã ngừng kinh doanh hoặc tạm ẩn")
                            .build());
                    continue;
                }

                int availableStock = getAvailableStock(variant.getVariantId());
                if (availableStock <= 0) {
                    skippedItems.add(CartSyncResponse.SkippedItem.builder()
                            .variantId(variant.getVariantId())
                            .requestedQuantity(guestItem.getQuantity())
                            .reason("Sản phẩm đã hết hàng trong kho")
                            .build());
                    continue;
                }

                Optional<CartItem> existingOpt = cartItemRepository.findByCartIdAndVariantId(cart.getCartId(), variant.getVariantId());
                if (existingOpt.isPresent()) {
                    // Idempotent: thay vì liên tục cộng dồn, lấy số lượng lớn nhất giữa giỏ hiện tại và giỏ khách gửi
                    CartItem existing = existingOpt.get();
                    int targetQty = Math.max(existing.getQuantity(), guestItem.getQuantity());
                    int finalQty = Math.min(targetQty, availableStock);
                    existing.setQuantity(finalQty);
                    existing.setUpdatedAt(LocalDateTime.now());
                    cartItemRepository.save(existing);
                    syncedCount++;
                } else {
                    int addQty = Math.min(guestItem.getQuantity(), availableStock);
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
                .skippedItems(skippedItems)
                .build();
    }

    // =========================================================================
    // PRIVATE HELPERS
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

    private PriceInfo resolvePriceWithMap(ProductVariant variant, Map<Long, PromotionVariant> promoMap) {
        if (variant == null) {
            return new PriceInfo(BigDecimal.ZERO, BigDecimal.ZERO, false);
        }

        BigDecimal listPrice = variant.getPrice() != null ? variant.getPrice() :
                (variant.getProduct() != null && variant.getProduct().getBasePrice() != null
                        ? variant.getProduct().getBasePrice() : BigDecimal.ZERO);

        if (promoMap.containsKey(variant.getVariantId())) {
            PromotionVariant pv = promoMap.get(variant.getVariantId());
            if (pv != null && pv.getPromotionalPrice() != null) {
                return new PriceInfo(listPrice, pv.getPromotionalPrice(), true);
            }
        }

        return new PriceInfo(listPrice, listPrice, false);
    }

    private BigDecimal calculateSubtotal(Long cartId) {
        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            PriceInfo priceInfo = resolvePrice(item.getVariant());
            subtotal = subtotal.add(priceInfo.currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subtotal;
    }
}
