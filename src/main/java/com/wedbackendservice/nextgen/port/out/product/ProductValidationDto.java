package com.wedbackendservice.nextgen.port.out.product;

import com.wedbackendservice.nextgen.entity.ProductVariant;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductValidationDto {
  private final Long variantId;
  private final String sku;
  private final String variantName;
  private final String productName;
  private final Integer romGb;
  private final BigDecimal originalPrice;
  private final BigDecimal promotionalPrice;
  private final BigDecimal effectivePrice;
  private final ProductVariant variantEntity;
}
