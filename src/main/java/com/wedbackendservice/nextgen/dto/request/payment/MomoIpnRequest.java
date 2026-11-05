package com.wedbackendservice.nextgen.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Body IPN MoMo gửi về server (POST JSON).
 * MoMo không gửi accessKey; field này lấy từ cấu hình khi verify chữ ký.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MomoIpnRequest {

    @NotBlank(message = "partnerCode không được để trống")
    String partnerCode;

    @NotBlank(message = "orderId không được để trống")
    String orderId;

    String requestId;
    Long amount;
    String orderInfo;
    String orderType;
    Long transId;
    Integer resultCode;
    String message;
    String payType;
    Long responseTime;
    String extraData;

    @NotBlank(message = "signature không được để trống")
    String signature;

    /**
     * Chuyển về Map<String, String> đúng bộ field MoMo dùng để ký.
     */
    public Map<String, String> toParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("partnerCode", partnerCode);
        params.put("orderId", orderId);
        params.put("requestId", requestId);
        params.put("amount", amount == null ? "" : String.valueOf(amount));
        params.put("orderInfo", orderInfo);
        params.put("orderType", orderType);
        params.put("transId", transId == null ? "" : String.valueOf(transId));
        params.put("resultCode", resultCode == null ? "" : String.valueOf(resultCode));
        params.put("message", message);
        params.put("payType", payType);
        params.put("responseTime", responseTime == null ? "" : String.valueOf(responseTime));
        params.put("extraData", extraData);
        params.put("signature", signature);
        return params;
    }
}
