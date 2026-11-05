package com.wedbackendservice.nextgen.dto.session;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

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
