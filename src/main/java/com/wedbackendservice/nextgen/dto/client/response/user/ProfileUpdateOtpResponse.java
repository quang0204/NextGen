package com.wedbackendservice.nextgen.dto.client.response.user;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileUpdateOtpResponse {
    String otpSessionToken;
    String targetEmailMasked;
    Integer expiresInSeconds;
    Integer resendCooldownSeconds;
}
