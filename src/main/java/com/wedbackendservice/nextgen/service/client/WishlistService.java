package com.wedbackendservice.nextgen.service.client;

import com.wedbackendservice.nextgen.dto.client.request.wishlist.WishlistToggleRequest;
import com.wedbackendservice.nextgen.dto.client.response.wishlist.WishlistListResponse;
import com.wedbackendservice.nextgen.dto.client.response.wishlist.WishlistProductResponse;
import com.wedbackendservice.nextgen.dto.client.response.wishlist.WishlistToggleResponse;
import com.wedbackendservice.nextgen.dto.common.PageInfoResponse;
import com.wedbackendservice.nextgen.entity.Product;
import com.wedbackendservice.nextgen.entity.ProductImage;
import com.wedbackendservice.nextgen.entity.ProductVariant;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.entity.Wishlist;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.ProductImageRepository;
import com.wedbackendservice.nextgen.repository.ProductRepository;
import com.wedbackendservice.nextgen.repository.ProductSpecRepository;
import com.wedbackendservice.nextgen.repository.ProductVariantRepository;
import com.wedbackendservice.nextgen.repository.WishlistRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSpecRepository productSpecRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SecurityHelper securityHelper;

    /**
     * API 13.1: danh sách sản phẩm yêu thích.
     */
    @Transactional(readOnly = true)
    public WishlistListResponse getMyWishlist(int page, int size) {
        User user = securityHelper.getCurrentUser();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Wishlist> result = wishlistRepository.findByUser_UserId(user.getUserId(), pageable);
        List<Wishlist> wishlists = result.getContent();

        List<Long> productIds = wishlists.stream()
                .map(Wishlist::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getProductId)
                .distinct()
                .toList();

        Map<Long, List<String>> imagesByProduct = productIds.isEmpty() ? Map.of() :
                productImageRepository.findByProduct_ProductIdInAndIsPrimaryTrue(productIds).stream()
                        .filter(img -> img.getProduct() != null)
                        .collect(Collectors.groupingBy(
                                img -> img.getProduct().getProductId(),
                                Collectors.mapping(ProductImage::getImageUrl, Collectors.toList())));

        Map<Long, List<String>> specsByProduct = productIds.isEmpty() ? Map.of() :
                productSpecRepository.findByProduct_ProductIdIn(productIds).stream()
                        .filter(s -> s.getProduct() != null)
                        .collect(Collectors.groupingBy(
                                s -> s.getProduct().getProductId(),
                                Collectors.mapping(s -> s.getSpecName() + ": " + s.getSpecValue(), Collectors.toList())));

        Map<Long, List<ProductVariant>> variantsByProduct = productIds.isEmpty() ? Map.of() :
                productVariantRepository.findByProduct_ProductIdIn(productIds).stream()
                        .filter(v -> v.getProduct() != null)
                        .collect(Collectors.groupingBy(v -> v.getProduct().getProductId()));

        return WishlistListResponse.builder()
                .items(wishlists.stream().map(w -> toResponse(w, imagesByProduct, specsByProduct, variantsByProduct)).toList())
                .total(result.getTotalElements())
                .pageInfo(PageInfoResponse.of(safePage, safeSize, result))
                .build();
    }

    /**
     * API 13.2: thêm hoặc bỏ yêu thích.
     */
    @Transactional
    public WishlistToggleResponse toggle(WishlistToggleRequest request) {
        User user = securityHelper.getCurrentUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Sản phẩm không tồn tại."));

        return wishlistRepository.findByUser_UserIdAndProduct_ProductId(user.getUserId(), product.getProductId())
                .map(existing -> {
                    wishlistRepository.delete(existing);
                    return WishlistToggleResponse.builder()
                            .productId(product.getProductId())
                            .isFavorite(false)
                            .message("Đã bỏ sản phẩm khỏi danh sách yêu thích.")
                            .build();
                })
                .orElseGet(() -> {
                    wishlistRepository.save(Wishlist.builder()
                            .user(user)
                            .product(product)
                            .build());
                    return WishlistToggleResponse.builder()
                            .productId(product.getProductId())
                            .isFavorite(true)
                            .message("Đã thêm sản phẩm vào danh sách yêu thích.")
                            .build();
                });
    }

    private WishlistProductResponse toResponse(
            Wishlist wishlist,
            Map<Long, List<String>> imagesByProduct,
            Map<Long, List<String>> specsByProduct,
            Map<Long, List<ProductVariant>> variantsByProduct) {
        Product product = wishlist.getProduct();
        if (product == null) {
            return WishlistProductResponse.builder()
                    .wishlistId(wishlist.getWishlistId())
                    .addedAt(wishlist.getCreatedAt())
                    .build();
        }

        Long productId = product.getProductId();
        List<String> images = imagesByProduct.getOrDefault(productId, List.of());
        List<String> specs = specsByProduct.getOrDefault(productId, List.of());
        List<ProductVariant> variants = variantsByProduct.getOrDefault(productId, List.of());

        BigDecimal priceFrom = variants.stream()
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(product.getBasePrice());

        return WishlistProductResponse.builder()
                .wishlistId(wishlist.getWishlistId())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .slug(product.getSlug())
                .thumbnailUrl(product.getThumbnailUrl())
                .basePrice(product.getBasePrice())
                .priceFrom(priceFrom)
                .status(product.getStatus())
                .images(images)
                .specs(specs)
                .variantCount(variants.size())
                .addedAt(wishlist.getCreatedAt())
                .build();
    }
}
