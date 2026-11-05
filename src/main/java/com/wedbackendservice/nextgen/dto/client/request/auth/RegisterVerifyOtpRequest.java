package com.wedbackendservice.nextgen.dto.client.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterVerifyOtpRequest {
    @NotBlank(message = "registrationToken không được để trống")
    String registrationToken;

    @NotBlank(message = "Mã OTP không được để trống")
    String otpCode;
}
