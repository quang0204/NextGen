package com.wedbackendservice.nextgen.dto.portal.request.product;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProductRequest {
    @Size(min = 3, max = 255, message = "Tên sản phẩm từ 3 đến 255 ký tự")
    String name;

    Long categoryId;
    Long brandId;
    String description;
    String warrantyPolicy;

    List<UpdateSpecItem> specs;
    List<UpdateImageItem> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UpdateSpecItem {
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
    public static class UpdateImageItem {
        Long imageId;
        String imageUrl;
        Boolean isPrimary;
        Integer sortOrder;
    }
}
