package com.wedbackendservice.nextgen.dto.portal.request.voucher;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class CreateVoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    String code;

    @NotBlank(message = "Loại giảm giá (discountType) không được để trống (FIXED hoặc PERCENT)")
    String discountType;

    @NotNull(message = "Mức giảm (discountValue) không được để trống")
    @DecimalMin(value = "0.0", message = "Mức giảm không được nhỏ hơn 0")
    BigDecimal discountValue;

    BigDecimal maxDiscount;

    BigDecimal minOrderValue;

    @NotNull(message = "Hạn mức sử dụng (usageLimit) không được để trống")
    @Min(value = 1, message = "Hạn mức sử dụng phải lớn hơn 0")
    Integer usageLimit;

    @NotBlank(message = "Thời gian bắt đầu (startAt) không được để trống")
    String startAt;

    @NotBlank(message = "Thời gian hết hạn (expiredAt) không được để trống")
    String expiredAt;

    @Builder.Default
    Boolean isActive = true;
}
