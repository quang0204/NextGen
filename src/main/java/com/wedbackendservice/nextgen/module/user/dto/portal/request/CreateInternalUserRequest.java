package com.wedbackendservice.nextgen.module.user.dto.portal.request;

import com.wedbackendservice.nextgen.module.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateInternalUserRequest {
  @NotBlank(message = "Họ và tên không được để trống")
  String fullName;

  @NotBlank(message = "Email không được để trống")
  @Email(message = "Email không đúng định dạng")
  String email;

  @NotBlank(message = "Số điện thoại không được để trống")
  @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
  String phone;

  @NotBlank(message = "Mật khẩu không được để trống")
  @Pattern(
      regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$",
      message = "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
  )
  String password;

  @NotNull(message = "Vai trò không được để trống")
  Role role;
}
