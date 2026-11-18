package com.wedbackendservice.nextgen.module.order.port.out.product;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

public interface ProductCatalogPort {
  Map<Long, ProductValidationDto> validateAndFetchPricing(
      Collection<Long> variantIds, LocalDateTime atTime);
}
