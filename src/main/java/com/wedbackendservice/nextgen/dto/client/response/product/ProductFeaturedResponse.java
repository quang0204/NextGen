package com.wedbackendservice.nextgen.dto.client.response.product;

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
public class ProductFeaturedResponse {
    Long productId;
    String name;
    String primaryImage;
    BigDecimal originalPrice;
    BigDecimal salePrice;
    Integer discountPercent;
    PromotionSummary promotion;
    Boolean inStock;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PromotionSummary {
        Long promotionId;
        String name;
        String endAt;
    }
}
