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
public class ConfirmOrderRequest {
    @NotBlank(message = "Đơn vị vận chuyển (carrier) không được để trống")
    String carrier;

    String note;
}
