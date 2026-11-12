package com.wedbackendservice.nextgen.dto.portal.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class CreateProductRequest {
    @NotNull(message = "categoryId không được để trống")
    @Positive(message = "categoryId phải là số nguyên dương")
    Long categoryId;

    @NotNull(message = "brandId không được để trống")
    @Positive(message = "brandId phải là số nguyên dương")
    Long brandId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 3, max = 255, message = "Tên sản phẩm từ 3 đến 255 ký tự")
    String name;

    String description;
    String warrantyPolicy;
    String status;

    List<ImageItemRequest> images;
    List<SpecItemRequest> specs;

    @NotEmpty(message = "Danh sách biến thể không được để trống (tối thiểu 1 biến thể)")
    @Valid
    List<VariantItemRequest> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ImageItemRequest {
        String uploadId;
        String imageUrl;
        Boolean isPrimary;
        Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SpecItemRequest {
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
    public static class VariantItemRequest {
        @NotBlank(message = "SKU không được để trống")
        String sku;

        String color;
        Integer ramGb;
        Integer romGb;
        String connectionType;
        String imageUrl;
        Map<String, Object> attributes;

        @NotNull(message = "listPrice không được để trống")
        @Positive(message = "listPrice phải lớn hơn 0")
        BigDecimal listPrice;

        @NotNull(message = "initialOnHandQty không được để trống")
        @PositiveOrZero(message = "initialOnHandQty phải >= 0")
        Integer initialOnHandQty;

        @PositiveOrZero(message = "minThreshold phải >= 0")
        Integer minThreshold;
    }
}
