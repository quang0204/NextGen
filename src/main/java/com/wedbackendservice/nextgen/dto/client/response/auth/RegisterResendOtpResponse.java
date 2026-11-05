package com.wedbackendservice.nextgen.dto.client.response.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterResendOtpResponse {
    Integer cooldownSeconds;
    Integer expiresInSeconds;
}
