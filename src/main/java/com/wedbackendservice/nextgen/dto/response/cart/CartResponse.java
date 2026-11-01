package com.wedbackendservice.nextgen.dto.response.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {
    Long cartId;
    Long userId;
    List<CartItemResponse> items;
    Integer totalQuantity;
    BigDecimal subtotal;
    LocalDateTime updatedAt;
}
