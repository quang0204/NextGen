package com.wedbackendservice.nextgen.module.shipment.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ShippingFeeCalculator {

  public static final BigDecimal EXPRESS_FEE = BigDecimal.valueOf(30000);
  public static final BigDecimal STANDARD_FEE = BigDecimal.ZERO;

  /**
   * Tính phí giao hàng độc lập tại Server dựa theo hình thức vận chuyển. Tuyệt đối không nhận giá
   * trị từ Client gửi lên.
   */
  public BigDecimal calculateFee(String shippingMethod) {
    if (shippingMethod != null && "EXPRESS".equalsIgnoreCase(shippingMethod.trim())) {
      return EXPRESS_FEE;
    }
    return STANDARD_FEE;
  }
}
