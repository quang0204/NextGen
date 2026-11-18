package com.wedbackendservice.nextgen.core.dto.session;

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
}
