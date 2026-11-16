package com.wedbackendservice.nextgen.dto.client.request.returnrequest;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnItemRequest {
    Long variantId;
    String productName;
    String sku;
    Integer quantity;
    BigDecimal unitPrice;
}
