package com.wedbackendservice.nextgen.core.dto.session;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileUpdateSessionData {
  Long userId;
  String fullName;
  String phone;
  String email;
  String otpCode;
  Instant createdAt;
  Instant lastSentAt;
  Instant otpExpiresAt;
}
