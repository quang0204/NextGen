package com.wedbackendservice.nextgen.module.order.port.out.voucher;

import com.wedbackendservice.nextgen.module.voucher.entity.Voucher;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoucherApplyResult {
  private final Long voucherId;
  private final String voucherCode;
  private final BigDecimal discountAmount;
  private final Voucher voucherEntity;
}
