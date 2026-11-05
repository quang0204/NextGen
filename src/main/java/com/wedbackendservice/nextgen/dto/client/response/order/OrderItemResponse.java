package com.wedbackendservice.nextgen.dto.client.response.order;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemResponse {
    Long orderItemId;
    Long variantId;
    Long productId;
    String productNameSnapshot;
    String skuSnapshot;
    String colorSnapshot;
    String romSnapshot;
    BigDecimal unitPrice;
    Integer quantity;
    BigDecimal lineTotal;
    String imageUrl;
}
