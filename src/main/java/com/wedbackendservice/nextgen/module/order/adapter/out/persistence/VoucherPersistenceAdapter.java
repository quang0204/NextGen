package com.wedbackendservice.nextgen.module.order.adapter.out.persistence;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.module.order.port.out.voucher.VoucherApplyResult;
import com.wedbackendservice.nextgen.module.order.port.out.voucher.VoucherPort;
import com.wedbackendservice.nextgen.module.voucher.entity.Voucher;
import com.wedbackendservice.nextgen.module.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoucherPersistenceAdapter implements VoucherPort {

  private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("#,###");
  private final VoucherRepository voucherRepository;

  @Override
  public VoucherApplyResult applyVoucherAtomic(
      String vCode, BigDecimal subtotal, LocalDateTime now) {
    if (vCode == null || vCode.isBlank()) {
      return null;
    }

    Voucher appliedVoucher =
        voucherRepository
            .findByVoucherCodeIgnoreCase(vCode.trim())
            .orElseThrow(
                () ->
                    new AppException(
                        ErrorCode.ERR_VOUCHER_NOT_FOUND,
                        "Mã giảm giá '" + vCode + "' không tồn tại!"));

    if (!Boolean.TRUE.equals(appliedVoucher.getIsActive())) {
      throw new AppException(
          ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + vCode + "' đã ngừng hoạt động!");
    }
    if (appliedVoucher.getStartDate() != null && now.isBefore(appliedVoucher.getStartDate())) {
      throw new AppException(
          ErrorCode.ERR_VOUCHER_EXPIRED,
          "Mã giảm giá '" + vCode + "' chưa đến thời gian có hiệu lực!");
    }
    if (appliedVoucher.getEndDate() != null && now.isAfter(appliedVoucher.getEndDate())) {
      throw new AppException(
          ErrorCode.ERR_VOUCHER_EXPIRED, "Mã giảm giá '" + vCode + "' đã hết hạn sử dụng!");
    }
    if (appliedVoucher.getMinOrderValue() != null
        && subtotal.compareTo(appliedVoucher.getMinOrderValue()) < 0) {
      throw new AppException(
          ErrorCode.ERR_VOUCHER_MIN_ORDER,
          String.format(
              "Đơn hàng chưa đạt giá trị tối thiểu %sđ để áp dụng mã %s!",
              CURRENCY_FORMATTER.format(appliedVoucher.getMinOrderValue()),
              appliedVoucher.getCode()));
    }
    if (appliedVoucher.getUsageLimit() != null
        && appliedVoucher.getUsedCount() != null
        && appliedVoucher.getUsedCount() >= appliedVoucher.getUsageLimit()) {
      throw new AppException(
          ErrorCode.ERR_VOUCHER_LIMIT_REACHED,
          "Mã giảm giá '" + vCode + "' đã hết lượt sử dụng trên hệ thống!");
    }

    int voucherUpdated = voucherRepository.incrementUsedCountAtomic(appliedVoucher.getVoucherId());
    if (voucherUpdated == 0) {
      throw new AppException(
          ErrorCode.ERR_VOUCHER_LIMIT_REACHED, "Mã giảm giá '" + vCode + "' vừa hết lượt sử dụng!");
    }

    BigDecimal discountAmount = calculateDiscount(appliedVoucher, subtotal);

    return VoucherApplyResult.builder()
        .voucherId(appliedVoucher.getVoucherId())
        .voucherCode(appliedVoucher.getCode())
        .discountAmount(discountAmount)
        .voucherEntity(appliedVoucher)
        .build();
  }

  @Override
  public void releaseVoucher(Long voucherId) {
    if (voucherId != null) {
      voucherRepository.decrementUsedCountAtomic(voucherId);
    }
  }

  private BigDecimal calculateDiscount(Voucher appliedVoucher, BigDecimal subtotal) {
    if (appliedVoucher == null) return BigDecimal.ZERO;
    String discType =
        appliedVoucher.getDiscountType() != null
            ? appliedVoucher.getDiscountType().trim().toUpperCase()
            : "FIXED";
    BigDecimal voucherDiscount;
    if ("PERCENT".equals(discType) || "PERCENTAGE".equals(discType)) {
      BigDecimal pct =
          appliedVoucher.getDiscountValue() != null
              ? appliedVoucher.getDiscountValue()
              : BigDecimal.ZERO;
      voucherDiscount =
          subtotal.multiply(pct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
      if (appliedVoucher.getMaxDiscount() != null
          && appliedVoucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
        voucherDiscount = voucherDiscount.min(appliedVoucher.getMaxDiscount());
      }
    } else {
      BigDecimal fixedVal =
          appliedVoucher.getDiscountValue() != null
              ? appliedVoucher.getDiscountValue()
              : BigDecimal.ZERO;
      voucherDiscount = fixedVal.min(subtotal);
    }
    return voucherDiscount;
  }
}
