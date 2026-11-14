package com.wedbackendservice.nextgen.service.client;

import com.wedbackendservice.nextgen.dto.client.request.cart.CartItemAddRequest;
import com.wedbackendservice.nextgen.dto.client.request.cart.CartItemUpdateRequest;
import com.wedbackendservice.nextgen.dto.client.request.cart.CartSyncRequest;
import com.wedbackendservice.nextgen.dto.client.response.cart.*;
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
import java.util.function.Function;
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
     */
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        User user = securityHelper.getCurrentUser();
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getUserId());

        if (cartOpt.isEmpty()) {
            return buildEmptyCartResponse(null, user.getUserId());
        }

        Cart cart = cartOpt.get();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getCartId());

        if (items.isEmpty()) {
            return buildEmptyCartResponse(cart.getCartId(), user.getUserId());
        }

        List<Long> variantIds = extractVariantIds(items);
        List<Long> productIds = items.stream()
                .map(item -> (item.getVariant() != null && item.getVariant().getProduct() != null)
                        ? item.getVariant().getProduct().getProductId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Inventory> inventoryMap = getInventoryMap(variantIds);
        Map<Long, PromotionVariant> promoMap = getActivePromoMap(variantIds);
        Map<Long, String> imageMap = productImageRepository.findByProduct_ProductIdInAndIsPrimaryTrue(productIds).stream()
                .collect(Collectors.toMap(img -> img.getProduct().getProductId(), ProductImage::getImageUrl, (a, b) -> a));

        List<CartItemResponse> itemResponses = new ArrayList<>(items.size());
        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            Product product = variant != null ? variant.getProduct() : null;

            int availableStock = variant != null ? extractAvailableStock(inventoryMap.get(variant.getVariantId())) : 0;
            boolean inStock = availableStock >= item.getQuantity();

            PriceInfo priceInfo = resolvePriceWithMap(variant, promoMap);
            BigDecimal lineTotal = priceInfo.currentPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(lineTotal);
            totalQuantity += item.getQuantity();

            String imageUrl = resolveImageUrl(variant, product, imageMap);

            itemResponses.add(CartItemResponse.builder()
                    .cartItemId(item.getCartItemId())
                    .variantId(variant != null ? variant.getVariantId() : null)
                    .productId(product != null ? product.getProductId() : null)
                    .productName(product != null ? product.getProductName() : "Sản phẩm")
                    .sku(variant != null ? variant.getSku() : "")
                    .color(variant != null ? variant.getColor() : "")
                    .ramGb(variant != null ? variant.getRamGb() : null)
                    .romGb(variant != null ? variant.getRomGb() : null)
                    .imageUrl(imageUrl)
                    .listPrice(priceInfo.listPrice())
                    .currentPrice(priceInfo.currentPrice())
                    .isFlashSale(priceInfo.isFlashSale())
                    .quantity(item.getQuantity())
                    .availableStock(availableStock)
                    .lineTotal(lineTotal)
                    .inStock(inStock)
                    .addedAt(item.getCreatedAt())
                    .build());
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
    public CartItemAddResponse addItemToCart(CartItemAddRequest request) {
        User user = securityHelper.getCurrentUser();

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));

        validateVariantActiveForAdd(variant);

        int requestedQty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
        int availableStock = getAvailableStock(variant.getVariantId());

        Cart cart = getOrCreateCart(user);
        Optional<CartItem> existingOpt = cartItemRepository.findByCartIdAndVariantId(cart.getCartId(), variant.getVariantId());

        int targetQty = existingOpt.map(item -> item.getQuantity() + requestedQty).orElse(requestedQty);
        if (targetQty > availableStock) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Sản phẩm hiện chỉ còn " + availableStock + " máy, không đủ số lượng yêu cầu");
        }

        LocalDateTime now = LocalDateTime.now();
        CartItem savedItem;
        if (existingOpt.isPresent()) {
            CartItem existing = existingOpt.get();
            existing.setQuantity(targetQty);
            existing.setUpdatedAt(now);
            savedItem = cartItemRepository.save(existing);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(targetQty)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            savedItem = cartItemRepository.save(newItem);
        }

        touchCart(cart, now);

        int totalCartItems = cartItemRepository.sumQuantityByCartId(cart.getCartId());

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
    public CartItemUpdateResponse updateItemQuantity(Long itemId, CartItemUpdateRequest request) {
        User user = securityHelper.getCurrentUser();
        CartItem item = getOwnedCartItem(itemId, user.getUserId());
        Cart cart = item.getCart();
        Long variantId = item.getVariant().getVariantId();

        int requestedQty = request.getQuantity() != null ? request.getQuantity() : 0;
        LocalDateTime now = LocalDateTime.now();

        if (requestedQty <= 0) {
            cartItemRepository.delete(item);
            touchCart(cart, now);

            List<CartItem> remainingItems = cartItemRepository.findByCartId(cart.getCartId());
            return CartItemUpdateResponse.builder()
                    .cartItemId(itemId)
                    .variantId(variantId)
                    .quantity(0)
                    .lineTotal(BigDecimal.ZERO)
                    .newSubtotal(calculateSubtotalBatch(remainingItems))
                    .build();
        }

        int availableStock = getAvailableStock(variantId);
        if (requestedQty > availableStock) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                    "Kho hàng chỉ còn tối đa " + availableStock + " thiết bị khả dụng.");
        }

        item.setQuantity(requestedQty);
        item.setUpdatedAt(now);
        CartItem updated = cartItemRepository.save(item);
        touchCart(cart, now);

        // Lấy toàn bộ items trong giỏ và tính giá bằng Batch Map (Chỉ 1 query khuyến mãi thay vì N+1)
        List<CartItem> currentItems = cartItemRepository.findByCartId(cart.getCartId());
        Map<Long, PromotionVariant> promoMap = getActivePromoMap(extractVariantIds(currentItems));

        PriceInfo itemPrice = resolvePriceWithMap(updated.getVariant(), promoMap);
        BigDecimal lineTotal = itemPrice.currentPrice().multiply(BigDecimal.valueOf(updated.getQuantity()));
        BigDecimal newSubtotal = calculateSubtotalWithMap(currentItems, promoMap);

        return CartItemUpdateResponse.builder()
                .cartItemId(itemId)
                .variantId(variantId)
                .quantity(updated.getQuantity())
                .lineTotal(lineTotal)
                .newSubtotal(newSubtotal)
                .build();
    }

    /**
     * API 7.4: Xóa 1 biến thể SKU khỏi giỏ hàng (Authenticated)
     */
    @Transactional
    public CartItemDeleteResponse removeItemFromCart(Long itemId) {
        User user = securityHelper.getCurrentUser();
        CartItem item = getOwnedCartItem(itemId, user.getUserId());
        Cart cart = item.getCart();

        cartItemRepository.delete(item);
        touchCart(cart, LocalDateTime.now());

        // Chỉ query danh sách còn lại 1 lần duy nhất
        List<CartItem> remainingItems = cartItemRepository.findByCartId(cart.getCartId());
        BigDecimal newSubtotal = calculateSubtotalBatch(remainingItems);

        return CartItemDeleteResponse.builder()
                .deletedItemId(itemId)
                .remainingItems(remainingItems.size())
                .newSubtotal(newSubtotal)
                .build();
    }

    /**
     * API 7.5: Xóa sạch toàn bộ giỏ hàng (Authenticated)
     */
    @Transactional
    public void clearCart() {
        User user = securityHelper.getCurrentUser();
        cartRepository.findByUserId(user.getUserId()).ifPresent(cart -> {
            cartItemRepository.deleteAllByCartId(cart.getCartId());
            touchCart(cart, LocalDateTime.now());
        });
    }

    /**
     * API 7.6: Đồng bộ giỏ hàng vãng lai (Guest) vào tài khoản khi Đăng nhập (Authenticated)
     * Đã tối ưu Batch Query để loại bỏ N+1 trong vòng lặp.
     */
    @Transactional
    public CartSyncResponse syncCart(CartSyncRequest request) {
        User user = securityHelper.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        int syncedCount = 0;
        List<CartSyncResponse.SkippedItem> skippedItems = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (request != null && request.getGuestItems() != null && !request.getGuestItems().isEmpty()) {
            // Lọc các item hợp lệ từ request
            List<CartItemAddRequest> validGuestItems = request.getGuestItems().stream()
                    .filter(g -> g != null && g.getVariantId() != null && g.getQuantity() != null && g.getQuantity() > 0)
                    .toList();

            if (!validGuestItems.isEmpty()) {
                List<Long> guestVariantIds = validGuestItems.stream()
                        .map(CartItemAddRequest::getVariantId)
                        .distinct()
                        .toList();

                // Batch-fetch toàn bộ Variant, Inventory và CartItem hiện có trong 3 câu query
                Map<Long, ProductVariant> variantMap = productVariantRepository.findAllById(guestVariantIds).stream()
                        .collect(Collectors.toMap(ProductVariant::getVariantId, Function.identity()));

                Map<Long, Inventory> inventoryMap = getInventoryMap(guestVariantIds);

                Map<Long, CartItem> existingItemMap = cartItemRepository.findByCartId(cart.getCartId()).stream()
                        .filter(ci -> ci.getVariant() != null)
                        .collect(Collectors.toMap(ci -> ci.getVariant().getVariantId(), Function.identity(), (a, b) -> a));

                List<CartItem> itemsToSave = new ArrayList<>();

                for (CartItemAddRequest guestItem : validGuestItems) {
                    Long varId = guestItem.getVariantId();
                    ProductVariant variant = variantMap.get(varId);

                    if (variant == null) {
                        skippedItems.add(buildSkippedItem(varId, guestItem.getQuantity(), "Biến thể sản phẩm không tồn tại"));
                        continue;
                    }

                    if (isVariantOrProductInactive(variant)) {
                        skippedItems.add(buildSkippedItem(varId, guestItem.getQuantity(), "Sản phẩm đã ngừng kinh doanh hoặc tạm ẩn"));
                        continue;
                    }

                    int availableStock = extractAvailableStock(inventoryMap.get(varId));
                    if (availableStock <= 0) {
                        skippedItems.add(buildSkippedItem(varId, guestItem.getQuantity(), "Sản phẩm đã hết hàng trong kho"));
                        continue;
                    }

                    CartItem existing = existingItemMap.get(varId);
                    if (existing != null) {
                        int targetQty = Math.max(existing.getQuantity(), guestItem.getQuantity());
                        existing.setQuantity(Math.min(targetQty, availableStock));
                        existing.setUpdatedAt(now);
                        itemsToSave.add(existing);
                    } else {
                        CartItem newItem = CartItem.builder()
                                .cart(cart)
                                .variant(variant)
                                .quantity(Math.min(guestItem.getQuantity(), availableStock))
                                .createdAt(now)
                                .updatedAt(now)
                                .build();
                        existingItemMap.put(varId, newItem); // Phòng trường hợp guestItems gửi trùng variantId
                        itemsToSave.add(newItem);
                    }
                    syncedCount++;
                }

                if (!itemsToSave.isEmpty()) {
                    cartItemRepository.saveAll(itemsToSave);
                }
            }
        }

        touchCart(cart, now);

        // Chỉ truy vấn lại danh sách 1 lần để tính cả totalItems và subtotal
        List<CartItem> currentItems = cartItemRepository.findByCartId(cart.getCartId());
        int totalItems = currentItems.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal subtotal = calculateSubtotalBatch(currentItems);

        return CartSyncResponse.builder()
                .cartId(cart.getCartId())
                .totalItems(totalItems)
                .syncedCount(syncedCount)
                .subtotal(subtotal)
                .skippedItems(skippedItems)
                .build();
    }

    // =========================================================================
    // PRIVATE & PUBLIC HELPERS
    // =========================================================================

    private CartResponse buildEmptyCartResponse(Long cartId, Long userId) {
        return CartResponse.builder()
                .cartId(cartId)
                .userId(userId)
                .items(List.of())
                .totalQuantity(0)
                .subtotal(BigDecimal.ZERO)
                .build();
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getUserId())
                .orElseGet(() -> {
                    try {
                        LocalDateTime now = LocalDateTime.now();
                    return cartRepository.save(Cart.builder()
                            .user(user)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                    } catch (Exception ex) {
                        return cartRepository.findByUserId(user.getUserId())
                                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể khởi tạo giỏ hàng"));
                    }
                });
    }

    private void touchCart(Cart cart, LocalDateTime now) {
        cart.setUpdatedAt(now);
        cartRepository.save(cart);
    }

    private CartItem getOwnedCartItem(Long itemId, Long userId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        if (!item.getCart().getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return item;
    }

    private void validateVariantActiveForAdd(ProductVariant variant) {
        if (variant.getStatus() != null && !"ACTIVE".equalsIgnoreCase(variant.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Biến thể sản phẩm này hiện đã ngừng kinh doanh hoặc tạm khóa.");
        }
        if (variant.getProduct() != null && variant.getProduct().getStatus() != null && !"ACTIVE".equalsIgnoreCase(variant.getProduct().getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_INACTIVE, "Sản phẩm hiện đã ngừng kinh doanh hoặc tạm ngừng phân phối.");
        }
    }

    private boolean isVariantOrProductInactive(ProductVariant variant) {
        return (variant.getStatus() != null && !"ACTIVE".equalsIgnoreCase(variant.getStatus()))
                || (variant.getProduct() != null && variant.getProduct().getStatus() != null && !"ACTIVE".equalsIgnoreCase(variant.getProduct().getStatus()));
    }

    public int getAvailableStock(Long variantId) {
        return inventoryRepository.findByVariant_VariantId(variantId)
                .map(this::extractAvailableStock)
                .orElse(0);
    }

    private int extractAvailableStock(Inventory inv) {
        if (inv == null) return 0;
        int onHand = inv.getQuantity() != null ? inv.getQuantity() : 0;
        int reserved = inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0;
        return Math.max(0, onHand - reserved);
    }

    private List<Long> extractVariantIds(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getVariant() != null ? item.getVariant().getVariantId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Long, Inventory> getInventoryMap(List<Long> variantIds) {
        if (variantIds.isEmpty()) return Map.of();
        return inventoryRepository.findByVariant_VariantIdIn(variantIds).stream()
                .collect(Collectors.toMap(inv -> inv.getVariant().getVariantId(), Function.identity(), (a, b) -> a));
    }

    private Map<Long, PromotionVariant> getActivePromoMap(List<Long> variantIds) {
        if (variantIds.isEmpty()) return Map.of();
        return promotionVariantRepository.findActivePromotionsForVariantIds(variantIds, LocalDateTime.now()).stream()
                .collect(Collectors.toMap(pv -> pv.getVariant().getVariantId(), Function.identity(), (a, b) -> a));
    }

    private String resolveImageUrl(ProductVariant variant, Product product, Map<Long, String> imageMap) {
        if (variant != null && variant.getImageUrl() != null && !variant.getImageUrl().isBlank()) {
            return variant.getImageUrl();
        }
        if (product != null) {
            return imageMap.get(product.getProductId());
        }
        return null;
    }

    private CartSyncResponse.SkippedItem buildSkippedItem(Long variantId, Integer requestedQty, String reason) {
        return CartSyncResponse.SkippedItem.builder()
                .variantId(variantId)
                .requestedQuantity(requestedQty)
                .reason(reason)
                .build();
    }

    public record PriceInfo(BigDecimal listPrice, BigDecimal currentPrice, boolean isFlashSale) {}

    public PriceInfo resolvePrice(ProductVariant variant) {
        if (variant == null) {
            return new PriceInfo(BigDecimal.ZERO, BigDecimal.ZERO, false);
        }
        BigDecimal listPrice = extractListPrice(variant);
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
        BigDecimal listPrice = extractListPrice(variant);
        PromotionVariant pv = promoMap.get(variant.getVariantId());
        if (pv != null && pv.getPromotionalPrice() != null) {
            return new PriceInfo(listPrice, pv.getPromotionalPrice(), true);
        }
        return new PriceInfo(listPrice, listPrice, false);
    }

    private BigDecimal extractListPrice(ProductVariant variant) {
        if (variant.getPrice() != null) {
            return variant.getPrice();
        }
        if (variant.getProduct() != null && variant.getProduct().getBasePrice() != null) {
            return variant.getProduct().getBasePrice();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateSubtotalBatch(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<Long, PromotionVariant> promoMap = getActivePromoMap(extractVariantIds(items));
        return calculateSubtotalWithMap(items, promoMap);
    }

    private BigDecimal calculateSubtotalWithMap(List<CartItem> items, Map<Long, PromotionVariant> promoMap) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            PriceInfo priceInfo = resolvePriceWithMap(item.getVariant(), promoMap);
            subtotal = subtotal.add(priceInfo.currentPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subtotal;
    }
}