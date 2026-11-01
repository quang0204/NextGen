package com.wedbackendservice.nextgen.dto.response.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterVerifyOtpResponse {
    UserRegisterResponse user;
    @Builder.Default
    String tokenType = "Bearer";
    String accessToken;
    String refreshToken;
    Long expiresIn;
}
