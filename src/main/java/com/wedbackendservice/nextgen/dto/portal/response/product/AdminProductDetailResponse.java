package com.wedbackendservice.nextgen.dto.portal.response.product;

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
public class AdminProductDetailResponse {
    Long productId;
    String name;
    Long categoryId;
    String categoryName;
    Long brandId;
    String brandName;
    String description;
    String warrantyPolicy;
    String status;
    List<VariantSchemaField> effectiveVariantSchema;
    List<AdminProductImageResponse> images;
    List<AdminProductSpecResponse> specs;
    List<AdminProductVariantResponse> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AdminProductImageResponse {
        Long imageId;
        String imageUrl;
        String objectKey;
        String bucket;
        Boolean isPrimary;
        Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AdminProductSpecResponse {
        Long specId;
        String groupName;
        String specName;
        String specValue;
        Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AdminProductVariantResponse {
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
        Boolean isActive;
        Boolean hasTransactions;
        AdminVariantInventoryResponse inventory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AdminVariantInventoryResponse {
        Long inventoryId;
        Integer onHandQty;
        Integer reservedQty;
        Integer availableQty;
        Integer minThreshold;
    }
}
