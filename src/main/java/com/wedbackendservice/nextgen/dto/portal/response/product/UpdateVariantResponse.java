package com.wedbackendservice.nextgen.dto.portal.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateVariantResponse {
    Long variantId;
    String sku;
    String variantName;
    String color;
    Integer ramGb;
    Integer romGb;
    String connectionType;
    Map<String, Object> attributes;
    Integer minThreshold;
    BigDecimal listPrice;
    BigDecimal salePrice;
    String imageUrl;
    Boolean isActive;
    String updatedAt;
}
