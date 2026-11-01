package com.wedbackendservice.nextgen.dto.session;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
