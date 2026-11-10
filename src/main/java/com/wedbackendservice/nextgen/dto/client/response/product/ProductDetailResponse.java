package com.wedbackendservice.nextgen.dto.client.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {
    Long productId;
    String name;
    ProductSummaryResponse.BrandSummary brand;
    ProductSummaryResponse.CategorySummary category;
    String description;
    String warrantyPolicy;
    String status;
    Integer soldCount;
    List<ProductImageResponse> images;
    List<SpecGroupResponse> specs;
    List<VariantDetailResponse> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ProductImageResponse {
        Long imageId;
        String imageUrl;
        Boolean isPrimary;
        Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SpecGroupResponse {
        String groupName;
        List<SpecItemResponse> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SpecItemResponse {
        String specName;
        String specValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VariantDetailResponse {
        Long variantId;
        String sku;
        String color;
        Integer ramGb;
        Integer romGb;
        String connectionType;
        BigDecimal listPrice;
        Integer availableQty;
        Boolean isActive;
    }
}
