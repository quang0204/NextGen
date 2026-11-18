package com.wedbackendservice.nextgen.module.user.dto.client.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForgotResendOtpRequest {
  @NotBlank(message = "resetToken không được để trống")
  String resetToken;
}
