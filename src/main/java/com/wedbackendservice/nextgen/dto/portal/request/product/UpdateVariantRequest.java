package com.wedbackendservice.nextgen.dto.portal.request.product;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateVariantRequest {
    @JsonAlias({"list_price", "listPrice", "compare_at_price", "compareAtPrice"})
    BigDecimal listPrice;

    @JsonAlias({"sale_price", "salePrice", "price"})
    BigDecimal salePrice;

    @JsonAlias({"compare_at_price", "compareAtPrice"})
    BigDecimal compareAtPrice;

    @JsonAlias({"price", "sale_price", "salePrice"})
    BigDecimal price;

    Boolean isActive;

    String sku;
    String color;
    Integer ramGb;
    Integer romGb;
    String connectionType;
    String imageUrl;
    Map<String, Object> attributes;
    Integer minThreshold;
}
