package com.wedbackendservice.nextgen.dto.response.voucher;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplyVoucherResponse {
    Long voucherId;
    String code;
    String discountType;
    BigDecimal discountValue;
    BigDecimal discountAmount;
    BigDecimal originalSubtotal;
    BigDecimal finalSubtotal;
}
