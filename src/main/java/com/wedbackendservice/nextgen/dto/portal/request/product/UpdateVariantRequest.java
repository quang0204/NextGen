package com.wedbackendservice.nextgen.dto.portal.request.product;

import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateVariantRequest {
    @Positive(message = "listPrice phải lớn hơn 0")
    BigDecimal listPrice;

    Boolean isActive;
}
