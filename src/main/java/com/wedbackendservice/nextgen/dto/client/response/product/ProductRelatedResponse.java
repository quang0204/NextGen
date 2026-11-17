package com.wedbackendservice.nextgen.dto.client.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
