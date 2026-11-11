package com.wedbackendservice.nextgen.dto.client.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wedbackendservice.nextgen.dto.client.response.category.VariantSchemaField;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    List<VariantSchemaField> variantSchema;
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
        String variantName;
        String color;
        Integer ramGb;
        Integer romGb;
        String connectionType;
        Map<String, Object> attributes;
        BigDecimal listPrice;
        BigDecimal salePrice;
        BigDecimal compareAtPrice;
        BigDecimal price;
        Integer availableQty;
        Boolean isActive;
    }
}
