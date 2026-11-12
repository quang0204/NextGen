package com.wedbackendservice.nextgen.dto.portal.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariantCreatedResponse {
    Long variantId;
    Long productId;
    String sku;
    String color;
    Integer ramGb;
    Integer romGb;
    BigDecimal listPrice;
    String imageUrl;
    Boolean isActive;
    CreateProductResponse.InventorySummary inventory;
    String createdAt;
}
