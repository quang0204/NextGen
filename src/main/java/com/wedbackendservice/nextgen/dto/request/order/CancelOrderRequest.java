package com.wedbackendservice.nextgen.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelOrderRequest {
    @NotBlank(message = "Lý do hủy đơn hàng không được để trống")
    String reason;
}
