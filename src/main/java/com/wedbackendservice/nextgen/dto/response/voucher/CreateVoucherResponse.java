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
public class CreateVoucherResponse {
    Long voucherId;
    String code;
    String discountType;
    BigDecimal discountValue;
    Integer usageLimit;
    Integer usedCount;
    Boolean isActive;
    String createdAt;
}
