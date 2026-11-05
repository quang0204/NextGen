package com.wedbackendservice.nextgen.dto.client.response.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemDeleteResponse {
    Long deletedItemId;
    Integer remainingItems;
    BigDecimal newSubtotal;
}
