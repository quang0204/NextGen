package com.wedbackendservice.nextgen.module.voucher.dto.portal.response;

import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminVoucherItemResponse {
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
}
