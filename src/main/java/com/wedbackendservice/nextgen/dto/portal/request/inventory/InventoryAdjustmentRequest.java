package com.wedbackendservice.nextgen.dto.portal.request.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryAdjustmentRequest {
    @NotNull(message = "variantId không được để trống")
    Long variantId;

    @NotNull(message = "Số lượng kiểm kê thực tế không được để trống")
    @Min(value = 0, message = "Số lượng kiểm kê thực tế không được nhỏ hơn 0")
    Integer actualCount;

    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    String reason;
}
