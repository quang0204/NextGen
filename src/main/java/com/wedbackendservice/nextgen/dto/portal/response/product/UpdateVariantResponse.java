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
public class UpdateVariantResponse {
    Long variantId;
    String sku;
    BigDecimal oldListPrice;
    BigDecimal newListPrice;
    Boolean isActive;
    String updatedAt;
}
