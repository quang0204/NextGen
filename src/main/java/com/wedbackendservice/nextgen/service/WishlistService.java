package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.request.wishlist.WishlistToggleRequest;
import com.wedbackendservice.nextgen.dto.response.PageInfoResponse;
import com.wedbackendservice.nextgen.dto.response.wishlist.WishlistListResponse;
import com.wedbackendservice.nextgen.dto.response.wishlist.WishlistProductResponse;
import com.wedbackendservice.nextgen.dto.response.wishlist.WishlistToggleResponse;
import com.wedbackendservice.nextgen.entity.Product;
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
import java.util.List;

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

        return WishlistListResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
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

    private WishlistProductResponse toResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        if (product == null) {
            return WishlistProductResponse.builder()
                    .wishlistId(wishlist.getWishlistId())
                    .addedAt(wishlist.getCreatedAt())
                    .build();
        }

        List<String> images = productImageRepository
                .findByProduct_ProductIdInAndIsPrimaryTrue(List.of(product.getProductId()))
                .stream()
                .map(image -> image.getImageUrl())
                .toList();

        List<String> specs = productSpecRepository.findByProduct_ProductId(product.getProductId())
                .stream()
                .map(spec -> spec.getSpecName() + ": " + spec.getSpecValue())
                .toList();

        List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(product.getProductId());
        BigDecimal priceFrom = variants.stream()
                .map(ProductVariant::getPrice)
                .filter(price -> price != null)
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
