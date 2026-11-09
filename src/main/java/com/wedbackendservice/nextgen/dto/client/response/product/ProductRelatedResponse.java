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
public class ProductRelatedResponse {
    Long productId;
    String name;
    String primaryImage;
    BigDecimal minPrice;
    String brand;
}
