package com.wedbackendservice.nextgen.dto.client.request.voucher;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplyVoucherRequest {
    @NotBlank(message = "Mã giảm giá (code) không được để trống")
    String code;

    @NotNull(message = "Tổng tiền đơn hàng (subtotal) không được để trống")
    @DecimalMin(value = "0.0", message = "Tổng tiền đơn hàng không được nhỏ hơn 0")
    BigDecimal subtotal;
}
