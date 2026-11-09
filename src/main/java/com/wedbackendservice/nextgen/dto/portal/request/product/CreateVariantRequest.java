package com.wedbackendservice.nextgen.dto.portal.request.product;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateVariantRequest {
    @NotBlank(message = "SKU không được để trống")
    String sku;

    String color;
    Integer ramGb;
    Integer romGb;
    String connectionType;

    @NotNull(message = "listPrice không được để trống")
    @Positive(message = "listPrice phải lớn hơn 0")
    BigDecimal listPrice;

    @NotNull(message = "initialOnHandQty không được để trống")
    @PositiveOrZero(message = "initialOnHandQty phải >= 0")
    Integer initialOnHandQty;

    @PositiveOrZero(message = "minThreshold phải >= 0")
    Integer minThreshold;
}
