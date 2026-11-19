package com.wedbackendservice.nextgen.module.product.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCompareResponse {
  ProductSummaryResponse.CategorySummary rootCategory;
  List<ProductDetailResponse> products;
}
