package com.wedbackendservice.nextgen.dto.request.cart;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemUpdateRequest {
    @NotNull(message = "Số lượng không được để trống")
    Integer quantity;
}
