package com.wedbackendservice.nextgen.dto.client.response.voucher;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidVoucherResponse {
    Long voucherId;
    String code;
    String discountType;
    BigDecimal discountValue;
    BigDecimal maxDiscount;
    BigDecimal minOrderValue;
    Integer usageLimit;
    Integer usedCount;
    Integer remainingCount;
    String startAt;
    String expiredAt;
}
