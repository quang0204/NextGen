package com.wedbackendservice.nextgen.module.user.dto.client.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
  @Pattern(
      regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$",
      message = "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
  )
  String newPassword;

  @NotBlank(message = "Mật khẩu xác nhận không được để trống")
  String confirmNewPassword;
}
