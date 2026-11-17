package com.wedbackendservice.nextgen.dto.client.response.voucher;

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
