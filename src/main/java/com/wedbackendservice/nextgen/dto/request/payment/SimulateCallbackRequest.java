package com.wedbackendservice.nextgen.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Mô phỏng callback từ cổng thanh toán (dùng cho demo/đồ án).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimulateCallbackRequest {

    @NotBlank(message = "transactionRef không được để trống")
    String transactionRef;

    /** true = thanh toán thành công, false = thất bại. Mặc định true. */
    Boolean success;

    BigDecimal amount;

    String note;
}
