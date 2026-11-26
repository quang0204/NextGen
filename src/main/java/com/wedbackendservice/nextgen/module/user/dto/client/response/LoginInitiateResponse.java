package com.wedbackendservice.nextgen.module.user.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wedbackendservice.nextgen.module.user.enums.OtpChannel;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginInitiateResponse {
  Boolean otpRequired;

  @Enumerated(EnumType.STRING)
  OtpChannel channel;

  String targetMasked;
  String sessionToken;
  Integer expiresInSeconds;
  Integer cooldownSeconds;

  // Được điền khi app.auth.login-otp-required = no (đăng nhập trực tiếp không qua OTP)
  UsersResponse user;
  String tokenType;
  String accessToken;
  String refreshToken;
  Long expiresIn;
}
