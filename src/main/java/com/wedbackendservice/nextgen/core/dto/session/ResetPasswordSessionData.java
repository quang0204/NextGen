package com.wedbackendservice.nextgen.core.dto.session;

import com.wedbackendservice.nextgen.module.user.enums.OtpChannel;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordSessionData {
  Long userId;
  String identity;
  OtpChannel channel;
  String target;
  String otpCode;
  LocalDateTime createdAt;
}
