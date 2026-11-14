package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.client.response.wishlist.WishlistProductResponse;
import com.wedbackendservice.nextgen.entity.Product;
import com.wedbackendservice.nextgen.entity.ProductVariant;
import com.wedbackendservice.nextgen.entity.Wishlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface WishlistMapper {
    @Mapping(source = "wishlist.wishlistId", target = "wishlistId")
    @Mapping(source = "wishlist.product.productId", target = "productId")
    @Mapping(source = "wishlist.product.productName", target = "productName")
    @Mapping(source = "wishlist.product.slug", target = "slug")
    @Mapping(source = "wishlist.product.thumbnailUrl", target = "thumbnailUrl")
    @Mapping(source = "wishlist.product.basePrice", target = "basePrice")
    @Mapping(source = "wishlist.product.status", target = "status")
    @Mapping(source = "wishlist.createdAt", target = "addedAt")
    @Mapping(source = "images", target = "images")
    @Mapping(source = "specs", target = "specs")
    @Mapping(target = "priceFrom", expression = "java(resolvePriceFrom(wishlist.getProduct(), variants))")
    @Mapping(target = "variantCount", expression = "java(variants != null ? variants.size() : 0)")
    WishlistProductResponse toResponse(Wishlist wishlist, List<String> images, List<String> specs, List<ProductVariant> variants);

    default BigDecimal resolvePriceFrom(Product product, List<ProductVariant> variants) {
        if (product == null) {
            return null;
        }
        if (variants == null || variants.isEmpty()) {
            return product.getBasePrice();
        }
        return variants.stream()
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(product.getBasePrice());
    }
}
