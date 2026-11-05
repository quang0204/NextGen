package com.wedbackendservice.nextgen.dto.client.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemRequest {
    @NotNull(message = "variantId không được để trống")
    Long variantId;

    @NotNull(message = "quantity không được để trống")
    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    Integer quantity;
}
