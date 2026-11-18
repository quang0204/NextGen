package com.wedbackendservice.nextgen.module.order.port.out.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemDto {
  private Long variantId;
  private Integer quantity;
}
