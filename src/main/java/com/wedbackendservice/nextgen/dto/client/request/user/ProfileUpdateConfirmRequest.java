package com.wedbackendservice.nextgen.dto.client.request.user;

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
public class ProfileUpdateConfirmRequest {
    @NotBlank(message = "otpSessionToken không được để trống")
    String otpSessionToken;

    @NotBlank(message = "otpCode không được để trống")
    @Pattern(regexp = "^[0-9]{6}$", message = "otpCode phải là chuỗi 6 chữ số")
    String otpCode;
}
