package com.wedbackendservice.nextgen.dto.request.promotion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionVariantItemRequest {
    @NotNull(message = "variantId không được để trống")
    Long variantId;

    @NotNull(message = "promoPrice không được để trống")
    @DecimalMin(value = "0.0", message = "promoPrice không được nhỏ hơn 0")
    BigDecimal promoPrice;
}
