package com.wedbackendservice.nextgen.dto.response.auth;

import com.wedbackendservice.nextgen.dto.response.UsersResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginVerifyOtpResponse {
    UsersResponse user;
    @Builder.Default
    String tokenType = "Bearer";
    String accessToken;
    String refreshToken;
    Long expiresIn;
}
