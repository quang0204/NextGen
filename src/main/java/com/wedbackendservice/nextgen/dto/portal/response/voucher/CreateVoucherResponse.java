package com.wedbackendservice.nextgen.dto.portal.response.voucher;

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
    BigDecimal maxDiscount;
    BigDecimal minOrderValue;
    Integer usageLimit;
    Integer usedCount;
    String startAt;
    String expiredAt;
    Boolean isActive;
    String createdAt;
}
