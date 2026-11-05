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
public class ResetPasswordRequest {
    @NotBlank(message = "resetToken không được để trống")
    String resetToken;

    @NotBlank(message = "Mã OTP không được để trống")
    String otpCode;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    String newPassword;

    @NotBlank(message = "Mật khẩu xác nhận không được để trống")
    String confirmNewPassword;
}
