package com.wedbackendservice.nextgen.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserProfileRequest {
    @NotBlank(message = "Họ và tên không được để trống")
    String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    String phone;
}
