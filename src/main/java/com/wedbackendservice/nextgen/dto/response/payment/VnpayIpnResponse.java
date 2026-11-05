package com.wedbackendservice.nextgen.dto.response.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response đúng format VNPay yêu cầu cho endpoint IPN.
 * Tên field phải giữ nguyên RspCode / Message.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VnpayIpnResponse {

    @JsonProperty("RspCode")
    String rspCode;

    @JsonProperty("Message")
    String message;

    public static VnpayIpnResponse of(String rspCode, String message) {
        return VnpayIpnResponse.builder()
                .rspCode(rspCode)
                .message(message)
                .build();
    }
}
