package com.wedbackendservice.nextgen.module.voucher.dto.client.response;

import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
