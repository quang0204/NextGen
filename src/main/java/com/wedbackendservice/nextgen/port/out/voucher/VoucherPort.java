package com.wedbackendservice.nextgen.port.out.voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface VoucherPort {
  VoucherApplyResult applyVoucherAtomic(String code, BigDecimal subtotal, LocalDateTime now);

  void releaseVoucher(Long voucherId);
}
