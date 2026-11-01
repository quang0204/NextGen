package com.wedbackendservice.nextgen.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginInitiateRequest {
    @NotBlank(message = "Số điện thoại hoặc Email không được để trống")
    String identity;

    @NotBlank(message = "Mật khẩu không được để trống")
    String password;
}
