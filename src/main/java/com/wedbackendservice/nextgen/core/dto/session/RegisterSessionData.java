package com.wedbackendservice.nextgen.core.dto.session;

import com.wedbackendservice.nextgen.module.user.dto.client.response.RegisterVerifyOtpResponse;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterSessionData {
  String fullName;
  String phone;
  String email;
  String passwordHash;
  String otpCode;
  LocalDateTime createdAt;
  @Builder.Default
  RegistrationStatus status = RegistrationStatus.PENDING;
  Long createdUserId;
  RegisterVerifyOtpResponse completedResponse;

  public RegistrationStatus getStatus() {
    return status != null ? status : RegistrationStatus.PENDING;
  }
}
